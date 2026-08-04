import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EndGameModalComponent } from './end-game-modal.component';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { ShareScorePayload } from '../../models/share-score.model';

describe('EndGameModalComponent', () => {
  let component: EndGameModalComponent;
  let fixture: ComponentFixture<EndGameModalComponent>;

  const mockPayload: ShareScorePayload = {
    date: '04/08/2026',
    score: 150,
    maxScore: 200,
    wordsFound: 15,
    totalWords: 30,
    mielegrammiFound: 2,
    totalMielegrammi: 3,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EndGameModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EndGameModalComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('payload', mockPayload);
    fixture.componentRef.setInput('isOpen', false);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create component instance', () => {
    expect(component).toBeTruthy();
  });

  it('should emit shareRequested output when onShare is called', () => {
    const spy = vi.fn();
    component.shareRequested.subscribe(spy);
    component.onShare();
    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit closed event when closed', () => {
    const spy = vi.fn();
    component.closed.subscribe(spy);
    component.closed.emit();
    expect(spy).toHaveBeenCalledTimes(1);
  });
});