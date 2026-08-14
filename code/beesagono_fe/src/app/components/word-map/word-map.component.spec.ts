import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WordMapComponent } from './word-map.component';

describe('WordMapComponent', () => {
  let component: WordMapComponent;
  let fixture: ComponentFixture<WordMapComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WordMapComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(WordMapComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
