import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { InvalidWordsComponent } from './invalid-words.component';
import { GameService } from '../../services/game/game.service';

describe('InvalidWordsComponent', () => {
  let component: InvalidWordsComponent;
  let fixture: ComponentFixture<InvalidWordsComponent>;

  let mockInvalidWordsSignal = signal<string[]>([]);
  let mockGameService: Partial<GameService>;

  beforeEach(async () => {
    mockInvalidWordsSignal = signal<string[]>([]);

    mockGameService = {
      invalidWords: mockInvalidWordsSignal,
    };

    await TestBed.configureTestingModule({
      imports: [InvalidWordsComponent],
      providers: [
        { provide: GameService, useValue: mockGameService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(InvalidWordsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with isExpanded set to true by default', () => {
    expect(component.isExpanded()).toBe(true);
  });

  it('should toggle isExpanded when toggleExpand() is called', () => {
    expect(component.isExpanded()).toBe(true);

    component.toggleExpand();
    expect(component.isExpanded()).toBe(false);

    component.toggleExpand();
    expect(component.isExpanded()).toBe(true);
  });

  it('should reflect invalidWords from GameService correctly', () => {
    expect(component.invalidWords()).toEqual([]);

    mockInvalidWordsSignal.set(['CASA', 'ALBERO']);
    fixture.detectChanges();

    expect(component.invalidWords()).toEqual(['CASA', 'ALBERO']);
  });
});