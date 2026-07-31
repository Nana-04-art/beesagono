import { TestBed } from '@angular/core/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should have title signal set to "beesagono"', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    // Verify signal value directly since template only contains router-outlet
    expect(app['title']()).toBe('beesagono');
  });
});
