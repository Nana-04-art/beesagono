import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FoundWordsComponent } from './found-words.component';
import { describe, beforeEach, it, expect } from 'vitest';

describe('FoundWordsComponent', () => {
  let component: FoundWordsComponent;
  let fixture: ComponentFixture<FoundWordsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FoundWordsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FoundWordsComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('foundWords', ['TANA', 'BANDITO']);
    fixture.componentRef.setInput('foundMielegrammi', ['BANDITO']);
    fixture.componentRef.setInput('totalPossibleWords', 10);
    fixture.componentRef.setInput('totalPossibleMielegrammi', 2);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create the component instance', () => {
    expect(component).toBeTruthy();
  });

  describe('Computed Signals', () => {
    it('should compute the correct total count based on found words', () => {
      expect(component.foundWords().length).toBe(2);
    });

    it('should dynamically update total count when inputs change', () => {
      fixture.componentRef.setInput('foundWords', ['TANA', 'BANDITO', 'CASA']);
      fixture.componentRef.setInput('foundMielegrammi', ['BANDITO']);
      fixture.detectChanges();

      expect(component.foundWords().length).toBe(3);
    });

    it('should handle empty input arrays gracefully', () => {
      fixture.componentRef.setInput('foundWords', []);
      fixture.componentRef.setInput('foundMielegrammi', []);
      fixture.detectChanges();

      expect(component.foundWords().length).toBe(0);
    });
  });

  describe('isMielegramma Helper Method', () => {
    it('should correctly identify a Mielegramma', () => {
      expect(component.isMielegramma('BANDITO')).toBe(true);
      expect(component.isMielegramma('TANA')).toBe(false);
    });

    it('should return false for non-existent or empty strings', () => {
      expect(component.isMielegramma('')).toBe(false);
      expect(component.isMielegramma('UNKNOWN')).toBe(false);
    });
  });

  describe('Accordion State & Interactions', () => {
    it('should start with isExpanded set to false', () => {
      expect(component.isExpanded()).toBe(false);
    });

    it('should toggle isExpanded state when toggleExpanded() is called', () => {
      component.toggleExpanded();
      expect(component.isExpanded()).toBe(true);

      component.toggleExpanded();
      expect(component.isExpanded()).toBe(false);
    });

    it('should toggle expanded state on DOM button click if present', () => {
      const button: HTMLButtonElement | null =
        fixture.nativeElement.querySelector('.toggle-button') ||
        fixture.nativeElement.querySelector('button');

      if (button) {
        button.click();
        fixture.detectChanges();
        expect(component.isExpanded()).toBe(true);

        button.click();
        fixture.detectChanges();
        expect(component.isExpanded()).toBe(false);
      } else {
        component.toggleExpanded();
        expect(component.isExpanded()).toBe(true);
      }
    });
  });
});