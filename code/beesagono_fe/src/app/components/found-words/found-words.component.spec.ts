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

    // Set initial input values
    fixture.componentRef.setInput('foundWords', ['APE', 'HONEY']);
    fixture.componentRef.setInput('foundMielegrammi', ['BEEHIVE']);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create the component instance', () => {
    expect(component).toBeTruthy();
  });

  it('should compute the correct total count including Mielegrammi', () => {
    expect(component.totalCount()).toBe(3);
  });

  it('should correctly identify a Mielegramma', () => {
    expect(component.isMielegramma('BEEHIVE')).toBe(true);
    expect(component.isMielegramma('APE')).toBe(false);
  });

  it('should toggle expanded state on click', () => {
    expect(component.isExpanded()).toBe(false);

    const button: HTMLButtonElement | null = fixture.nativeElement.querySelector('.toggle-button');
    button?.click();
    fixture.detectChanges();

    expect(component.isExpanded()).toBe(true);
  });
});