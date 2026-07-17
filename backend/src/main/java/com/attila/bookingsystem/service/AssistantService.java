package com.attila.bookingsystem.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUnion;
import com.anthropic.models.messages.ToolUseBlock;
import com.attila.bookingsystem.dto.assistant.ChatMessage;
import com.attila.bookingsystem.dto.assistant.ChatRequest;
import com.attila.bookingsystem.dto.assistant.ChatResponse;
import com.attila.bookingsystem.exception.ApiException;
import com.attila.bookingsystem.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A booking-asszisztens: a userrel folytatott beszélgetést és a Claude
 * tool-use hurkot vezérli. A beszélgetés-előzményt a kliens tartja fenn és
 * küldi el minden kéréssel (stateless, mint a JWT-s auth) - a szerver nem
 * tárol semmit kérések között.
 *
 * Az anthropicClient NULL, ha nincs beállítva ANTHROPIC_API_KEY - ez
 * szándékosan nem induláskor buktatja el az appot (a többi funkció ettől
 * függetlenül működjön), csak akkor derül ki, ha valaki ténylegesen
 * használni próbálja az asszisztenst.
 */
@Service
public class AssistantService {

    private static final int MAX_TOOL_ITERATIONS = 5;
    private static final long MAX_TOKENS = 1024L;

    private final AnthropicClient anthropicClient;
    private final AssistantToolExecutor toolExecutor;
    private final List<ToolUnion> tools;

    public AssistantService(@Value("${app.assistant.api-key:}") String apiKey, AssistantToolExecutor toolExecutor) {
        this.anthropicClient = (apiKey == null || apiKey.isBlank())
                ? null
                : AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.toolExecutor = toolExecutor;
        this.tools = AssistantTools.definitions().stream().map(ToolUnion::ofTool).toList();
    }

    public ChatResponse chat(ChatRequest request) {
        if (anthropicClient == null) {
            throw new BadRequestException("The AI assistant is not configured on this server (missing ANTHROPIC_API_KEY).");
        }

        List<MessageParam> messages = new ArrayList<>();
        for (ChatMessage message : request.messages()) {
            MessageParam.Role role = "assistant".equals(message.role()) ? MessageParam.Role.ASSISTANT : MessageParam.Role.USER;
            messages.add(MessageParam.builder().role(role).content(message.content()).build());
        }

        for (int iteration = 0; iteration < MAX_TOOL_ITERATIONS; iteration++) {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(Model.CLAUDE_HAIKU_4_5)
                    .maxTokens(MAX_TOKENS)
                    .system(systemPrompt())
                    .tools(tools)
                    .messages(messages)
                    .build();

            Message response = anthropicClient.messages().create(params);

            messages.add(MessageParam.builder()
                    .role(MessageParam.Role.ASSISTANT)
                    .contentOfBlockParams(response.content().stream().map(ContentBlock::toParam).toList())
                    .build());

            if (response.stopReason().isEmpty() || response.stopReason().get() != StopReason.TOOL_USE) {
                return new ChatResponse(extractText(response));
            }

            List<ContentBlockParam> toolResults = new ArrayList<>();
            for (ContentBlock block : response.content()) {
                block.toolUse().ifPresent(toolUse -> toolResults.add(ContentBlockParam.ofToolResult(runTool(toolUse))));
            }
            messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(toolResults).build());
        }

        return new ChatResponse("Sorry, that's taking more steps than I'm allowed - could you try rephrasing your request?");
    }

    private ToolResultBlockParam runTool(ToolUseBlock toolUse) {
        ToolResultBlockParam.Builder result = ToolResultBlockParam.builder().toolUseId(toolUse.id());
        try {
            Map<String, Object> input = toolUse._input().convert(Map.class);
            String output = switch (toolUse.name()) {
                case "search_time_slots" -> toolExecutor.searchTimeSlots(input);
                case "book_time_slot" -> toolExecutor.bookTimeSlot(input);
                case "list_my_bookings" -> toolExecutor.listMyBookings();
                case "cancel_booking" -> toolExecutor.cancelBooking(input);
                default -> throw new BadRequestException("Unknown tool: " + toolUse.name());
            };
            return result.content(output).build();
        } catch (ApiException e) {
            return result.content(e.getMessage()).isError(true).build();
        }
    }

    private String extractText(Message response) {
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(t -> {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(t.text());
            });
        }
        return text.toString();
    }

    private String systemPrompt() {
        return """
                You are a friendly booking assistant for a small-business appointment booking platform. \
                You help the CURRENT, already-authenticated user find services, book time slots, view their \
                own bookings, and cancel them - always through the provided tools, never by guessing IDs or \
                inventing information.

                Rules:
                - Before calling book_time_slot or cancel_booking, make sure the user actually confirmed which \
                  specific slot or booking they mean. If search_time_slots or list_my_bookings returned several \
                  candidates, list the relevant ones briefly and ask which one, unless the user's request already \
                  narrows it down to exactly one.
                - Never fabricate a timeSlotId or bookingId - only use ones returned by a tool call.
                - Keep replies short and conversational, not a wall of text.
                - The current date and time (UTC) is: %s
                """.formatted(Instant.now());
    }
}
