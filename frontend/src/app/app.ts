import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavComponent } from './shared/nav/nav.component';
import { ChatWidgetComponent } from './shared/chat-widget/chat-widget.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NavComponent, ChatWidgetComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {}
