import { Component, output, signal } from '@angular/core';
import { RANK_TIERS } from '../../../config/rank-tiers.config';

@Component({
  selector: 'app-rules',
  imports: [],
  templateUrl: './rules.component.html',
  styleUrl: './rules.component.scss',
})
export class RulesComponent {
  readonly rankTiers = RANK_TIERS;
}
