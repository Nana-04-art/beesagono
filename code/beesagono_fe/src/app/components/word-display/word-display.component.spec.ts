import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WordDisplayComponent } from './word-display.component';
import { describe, beforeEach, it, expect } from 'vitest';

describe('WordDisplayComponent', () => {
  let component: WordDisplayComponent;
  let fixture: ComponentFixture<WordDisplayComponent>;

  // Helper per pulire il testo dal cursore "|" e dagli spazi
  const getCleanText = (element: HTMLElement): string => {
    return (element.textContent || '')
      .replace(/\|/g, '')      // Rimuves the cursor character
      .replace(/\s+/g, '')     // Rimuoves all whitespace
      .trim();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WordDisplayComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WordDisplayComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('currentInput', 'APE');
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render the current input text correctly', () => {
    fixture.componentRef.setInput('currentInput', 'MIELE');
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(getCleanText(element)).toContain('MIELE');
  });

  it('should apply center letter highlighting if centerLetter is provided', () => {
    fixture.componentRef.setInput('currentInput', 'ALVEARE');
    fixture.componentRef.setInput('centerLetter', 'A');
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(getCleanText(element)).toContain('ALVEARE');
  });

  it('should display feedback message when provided', () => {
    fixture.componentRef.setInput('feedbackMessage', 'Ottimo!');
    fixture.componentRef.setInput('feedbackType', 'success');
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Ottimo!');
  });

  it('should handle error feedback type properly', () => {
    fixture.componentRef.setInput('feedbackMessage', 'Parola troppo corta');
    fixture.componentRef.setInput('feedbackType', 'error');
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Parola troppo corta');
  });
});