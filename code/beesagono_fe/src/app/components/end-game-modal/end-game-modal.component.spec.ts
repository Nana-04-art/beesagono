import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EndGameModalComponent } from './end-game-modal.component';
import { describe, beforeEach, it, expect, vi } from 'vitest';
import { ShareScorePayload } from '../../models/share-score.model';
import { ElementRef } from '@angular/core';

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

  const flushTimeout = () => new Promise((resolve) => setTimeout(resolve, 0));

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

  describe('Outputs', () => {
    it('should emit shareRequested output when onShare is called', () => {
      const spy = vi.fn();
      component.shareRequested.subscribe(spy);

      component.onShare();

      expect(spy).toHaveBeenCalledTimes(1);
    });

    it('should emit closed output when closeModal is called', () => {
      const spy = vi.fn();
      component.closed.subscribe(spy);

      component.closeModal();

      expect(spy).toHaveBeenCalledTimes(1);
    });
  });

  describe('Focus Management & Effects', () => {
    it('should save active element and focus share button when opened', async () => {
      const externalButton = document.createElement('button');
      document.body.appendChild(externalButton);
      externalButton.focus();
      expect(document.activeElement).toBe(externalButton);

      const mockShareBtn = document.createElement('button');
      const spyFocus = vi.spyOn(mockShareBtn, 'focus');
      vi.spyOn(component, 'shareButton').mockReturnValue(new ElementRef(mockShareBtn));
      fixture.componentRef.setInput('isOpen', true);
      fixture.detectChanges();

      await flushTimeout();

      expect(spyFocus).toHaveBeenCalled();

      document.body.removeChild(externalButton);
    });

    it('should restore previous focus when closed', async () => {
      const externalButton = document.createElement('button');
      document.body.appendChild(externalButton);
      externalButton.focus();
      fixture.componentRef.setInput('isOpen', true);
      fixture.detectChanges();
      await flushTimeout();
      fixture.componentRef.setInput('isOpen', false);
      fixture.detectChanges();

      expect(document.activeElement).toBe(externalButton);

      document.body.removeChild(externalButton);
    });
  });

  describe('Keyboard Listeners', () => {
    it('should emit closed output when Escape key is pressed and modal is open', () => {
      const spy = vi.fn();
      component.closed.subscribe(spy);
      fixture.componentRef.setInput('isOpen', true);
      fixture.detectChanges();

      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });
      const spyPreventDefault = vi.spyOn(escapeEvent, 'preventDefault');

      component.handleEscape(escapeEvent);

      expect(spyPreventDefault).toHaveBeenCalled();
      expect(spy).toHaveBeenCalledTimes(1);
    });

    it('should NOT emit closed output when Escape key is pressed and modal is closed', () => {
      const spy = vi.fn();
      component.closed.subscribe(spy);
      fixture.componentRef.setInput('isOpen', false);
      fixture.detectChanges();

      const escapeEvent = new KeyboardEvent('keydown', { key: 'Escape' });

      component.handleEscape(escapeEvent);

      expect(spy).not.toHaveBeenCalled();
    });

    describe('Tab Trap Focus Locking', () => {
      let container: HTMLDivElement;
      let btn1: HTMLButtonElement;
      let btn2: HTMLButtonElement;

      beforeEach(() => {
        container = document.createElement('div');
        btn1 = document.createElement('button');
        btn2 = document.createElement('button');
        container.appendChild(btn1);
        container.appendChild(btn2);
        document.body.appendChild(container);

        vi.spyOn(component, 'modalContainer').mockReturnValue(new ElementRef(container));
      });

      afterEach(() => {
        document.body.removeChild(container);
      });

      it('should do nothing on Tab press if modal is closed', () => {
        fixture.componentRef.setInput('isOpen', false);
        fixture.detectChanges();

        const tabEvent = new KeyboardEvent('keydown', { key: 'Tab' });
        const spyPreventDefault = vi.spyOn(tabEvent, 'preventDefault');

        component.handleTabTrap(tabEvent);

        expect(spyPreventDefault).not.toHaveBeenCalled();
      });

      it('should cycle focus from last to first element on TAB', () => {
        fixture.componentRef.setInput('isOpen', true);
        fixture.detectChanges();

        btn2.focus();
        const tabEvent = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: false });
        const spyPreventDefault = vi.spyOn(tabEvent, 'preventDefault');

        component.handleTabTrap(tabEvent);

        expect(spyPreventDefault).toHaveBeenCalled();
        expect(document.activeElement).toBe(btn1);
      });

      it('should cycle focus from first to last element on SHIFT + TAB', () => {
        fixture.componentRef.setInput('isOpen', true);
        fixture.detectChanges();

        btn1.focus();
        const tabEvent = new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true });
        const spyPreventDefault = vi.spyOn(tabEvent, 'preventDefault');

        component.handleTabTrap(tabEvent);

        expect(spyPreventDefault).toHaveBeenCalled();
        expect(document.activeElement).toBe(btn2);
      });
    });
  });
});