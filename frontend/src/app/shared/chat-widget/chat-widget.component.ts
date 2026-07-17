import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AssistantService } from '../../core/services/assistant.service';
import { AuthService } from '../../core/services/auth.service';
import { ChatMessage } from '../../core/models/assistant.models';
import { extractErrorMessage } from '../../core/utils/api-error.util';

@Component({
  selector: 'app-chat-widget',
  imports: [FormsModule],
  templateUrl: './chat-widget.component.html',
  styleUrl: './chat-widget.component.scss',
})
export class ChatWidgetComponent {
  private readonly assistantService = inject(AssistantService);
  private readonly authService = inject(AuthService);

  private readonly scrollAnchor = viewChild<ElementRef<HTMLElement>>('scrollAnchor');

  readonly isAuthenticated = this.authService.isAuthenticated;
  readonly open = signal(false);
  readonly messages = signal<ChatMessage[]>([]);
  readonly draft = signal('');
  readonly sending = signal(false);
  readonly errorMessage = signal<string | null>(null);

  toggle(): void {
    this.open.set(!this.open());
  }

  send(): void {
    const text = this.draft().trim();
    if (!text || this.sending()) {
      return;
    }

    const history = [...this.messages(), { role: 'user' as const, content: text }];
    this.messages.set(history);
    this.draft.set('');
    this.sending.set(true);
    this.errorMessage.set(null);
    this.scrollToBottom();

    this.assistantService.chat(history).subscribe({
      next: (response) => {
        this.messages.set([...this.messages(), { role: 'assistant', content: response.reply }]);
        this.sending.set(false);
        this.scrollToBottom();
      },
      error: (error: unknown) => {
        this.sending.set(false);
        this.errorMessage.set(extractErrorMessage(error, 'Could not reach the assistant.'));
      },
    });
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToBottom(): void {
    setTimeout(() => this.scrollAnchor()?.nativeElement.scrollIntoView({ behavior: 'smooth' }));
  }
}
