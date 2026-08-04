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

  it('should emit delete event when delete button is clicked', () => {
    const spy = vi.spyOn(component.deletePressed, 'emit');
    const deleteBtn = fixture.nativeElement.querySelector('.control-btn--secondary');

    deleteBtn.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit shuffle event when shuffle button is clicked', () => {
    const spy = vi.spyOn(component.shufflePressed, 'emit');
    const shuffleBtn = fixture.nativeElement.querySelector('.control-btn--icon');

    shuffleBtn.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('should emit submit event when submit button is clicked', () => {
    const spy = vi.spyOn(component.submitPressed, 'emit');
    const submitBtn = fixture.nativeElement.querySelector('.control-btn--primary');

    submitBtn.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });
});