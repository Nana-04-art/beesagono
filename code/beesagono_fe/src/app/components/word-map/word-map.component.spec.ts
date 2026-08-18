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
      providers: [
        { provide: GameService, useValue: mockGameService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WordMapComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('items', []);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});