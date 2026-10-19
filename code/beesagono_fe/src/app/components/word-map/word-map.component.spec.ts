import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WordMapComponent } from './word-map.component';
import { GameService } from '../../services/game/game.service';
import { signal } from '@angular/core';

describe('WordMapComponent', () => {
  let component: WordMapComponent;
  let fixture: ComponentFixture<WordMapComponent>;

  const mockGameService = {
    letterColors: signal({})
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WordMapComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WordMapComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('items', []);
    fixture.componentRef.setInput('letterColors', new Map<string, string>());

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('getLetterColor helper', () => {
    it('should return default fallback color if colors are missing or invalid', () => {
      fixture.componentRef.setInput('letterColors', null as any);
      const color = component.getLetterColor({ word: 'A', length: 1, initial: 'A', isFound: false, isPangram: false });
      expect(color).toBe('#e2e8f0');
    });

    it('should retrieve color correctly when letterColors is a Map', () => {
      const colorMap = new Map<string, string>([['A', '#ff0000']]);
      fixture.componentRef.setInput('letterColors', colorMap);

      const color = component.getLetterColor({ word: 'APE', length: 3, initial: 'A', isFound: true, isPangram: false });
      expect(color).toBe('#ff0000');
    });

    it('should retrieve color correctly when letterColors is a plain Record object', () => {
      const colorRecord: Record<string, string> = { B: '#00ff00' };
      fixture.componentRef.setInput('letterColors', colorRecord);

      const color = component.getLetterColor({ word: 'BARCA', length: 5, initial: 'B', isFound: true, isPangram: false });
      expect(color).toBe('#00ff00');
    });

    it('should fallback to default color if letter is not present in map or record', () => {
      fixture.componentRef.setInput('letterColors', new Map());
      const color = component.getLetterColor({ word: 'CASA', length: 4, initial: 'C', isFound: true, isPangram: false });
      expect(color).toBe('#e2e8f0');
    });
  });
});