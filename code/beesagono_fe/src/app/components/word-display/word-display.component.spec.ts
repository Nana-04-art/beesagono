import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WordDisplayComponent } from './word-display.component';

describe('WordDisplayComponent', () => {
  let component: WordDisplayComponent;
  let fixture: ComponentFixture<WordDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WordDisplayComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WordDisplayComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
