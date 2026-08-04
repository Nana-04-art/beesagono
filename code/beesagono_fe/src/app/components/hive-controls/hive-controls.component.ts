import { Component, output } from '@angular/core';

@Component({
  selector: 'app-hive-controls',
  imports: [],
  templateUrl: './hive-controls.component.html',
  styleUrl: './hive-controls.component.scss',
})
export class HiveControlsComponent {
  // Emitted when the user requests to delete the last character 
  readonly deletePressed = output<void>();

  // Emitted when the user requests to shuffle outer letters
  readonly shufflePressed = output<void>();

  // Emitted when the user submits the current word
  readonly submitPressed = output<void>();

  onDelete(): void {
    this.deletePressed.emit();
  }

  onShuffle(): void {
    this.shufflePressed.emit();
  }

  onSubmit(): void {
    this.submitPressed.emit();
  }
}