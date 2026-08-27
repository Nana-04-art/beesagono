import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HiveControlsComponent } from './hive-controls.component';
import { vi, describe, beforeEach, it, expect } from 'vitest';

describe('HiveControlsComponent', () => {
  let component: HiveControlsComponent;
  let fixture: ComponentFixture<HiveControlsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HiveControlsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HiveControlsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should emit deletePressed when onDelete is called', () => {
    const spy = vi.spyOn(component.deletePressed, 'emit');

    component.onDelete();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit shufflePressed when onShuffle is called', () => {
    const spy = vi.spyOn(component.shufflePressed, 'emit');

    component.onShuffle();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit submitPressed when onSubmit is called', () => {
    const spy = vi.spyOn(component.submitPressed, 'emit');

    component.onSubmit();

    expect(spy).toHaveBeenCalledTimes(1);
  });
});