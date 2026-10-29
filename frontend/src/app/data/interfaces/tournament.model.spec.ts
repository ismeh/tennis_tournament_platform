import {
  getAvailableStageOptions,
  getSurfaceBackgroundImage,
  getTournamentEventGenderLabel,
  getTournamentStageTypeLabel,
  getTournamentSurfaceCategoryLabel,
  isConsolationDisabled,
  isValidStageType,
  validateStageSequence
} from './tournament.model';

describe('tournament.model helpers', () => {
  it('returns labels and fallback values for surface categories', () => {
    expect(getTournamentSurfaceCategoryLabel('CLAY')).toBe('Tierra batida');
    expect(getTournamentSurfaceCategoryLabel('HARD')).toBe('Pista dura');
    expect(getTournamentSurfaceCategoryLabel('UNKNOWN' as never)).toBe('UNKNOWN');
  });

  it('returns background images and fallback values for surface categories', () => {
    expect(getSurfaceBackgroundImage('GRASS')).toBe('surfaces/grass.jpg');
    expect(getSurfaceBackgroundImage('CARPET')).toBe('surfaces/carpet.jpg');
    expect(getSurfaceBackgroundImage('UNKNOWN' as never)).toBe('');
  });

  it('returns labels and fallback values for event genders', () => {
    expect(getTournamentEventGenderLabel('MALE')).toBe('Masculino');
    expect(getTournamentEventGenderLabel('FEMALE')).toBe('Femenino');
    expect(getTournamentEventGenderLabel('UNKNOWN' as never)).toBe('UNKNOWN');
  });

  it('validates stage types and falls back for unknown values', () => {
    expect(isValidStageType('SINGLE_ELIMINATION')).toBeTrue();
    expect(isValidStageType('ROUND_ROBIN')).toBeTrue();
    expect(isValidStageType('INVALID')).toBeFalse();
    expect(getTournamentStageTypeLabel('CONSOLATION')).toBe('Consolación');
    expect(getTournamentStageTypeLabel('UNKNOWN' as never)).toBe('UNKNOWN');
  });

  it('flags invalid stage sequences and accepts valid ones', () => {
    expect(validateStageSequence([])).toEqual([
      { rule: 'EMPTY', message: 'Debe haber al menos una fase definida.' }
    ]);

    expect(validateStageSequence(['CONSOLATION'] as any)).toEqual([
      { rule: 'R1', message: 'La primera fase no puede ser CONSOLATION. Requiere jugadores eliminados en una fase previa.' }
    ]);

    expect(validateStageSequence(['SINGLE_ELIMINATION', 'CONSOLATION'])).toEqual([]);
    expect(validateStageSequence(['ROUND_ROBIN', 'CONSOLATION'] as any)).toEqual([
      {
        rule: 'R2',
        message: 'CONSOLATION en la fase 2 solo es válida si la fase anterior es SINGLE_ELIMINATION (actual: ROUND_ROBIN).'
      },
      {
        rule: 'MATRIX',
        message: "Transición inválida: 'ROUND_ROBIN' -> 'CONSOLATION'. Desde ROUND_ROBIN solo se permite: ROUND_ROBIN, SINGLE_ELIMINATION, DOUBLE_ELIMINATION."
      }
    ]);

    expect(validateStageSequence(['DOUBLE_ELIMINATION', 'CONSOLATION'])).toEqual([
      {
        rule: 'R2',
        message: 'CONSOLATION en la fase 2 solo es válida si la fase anterior es SINGLE_ELIMINATION (actual: DOUBLE_ELIMINATION).'
      },
      {
        rule: 'MATRIX',
        message: "Transición inválida: 'DOUBLE_ELIMINATION' -> 'CONSOLATION'. Desde DOUBLE_ELIMINATION solo se permite: ROUND_ROBIN, SINGLE_ELIMINATION."
      }
    ]);

    expect(validateStageSequence(['ROUND_ROBIN', 'DOUBLE_ELIMINATION', 'CONSOLATION'])).toEqual([
      {
        rule: 'R3',
        message: 'Si la fase 2 es DOUBLE_ELIMINATION, la fase 3 no puede ser CONSOLATION.'
      },
      {
        rule: 'R2',
        message: 'CONSOLATION en la fase 3 solo es válida si la fase anterior es SINGLE_ELIMINATION (actual: DOUBLE_ELIMINATION).'
      },
      {
        rule: 'MATRIX',
        message: "Transición inválida: 'DOUBLE_ELIMINATION' -> 'CONSOLATION'. Desde DOUBLE_ELIMINATION solo se permite: ROUND_ROBIN, SINGLE_ELIMINATION."
      }
    ]);

    expect(validateStageSequence(['CUSTOM', 'SINGLE_ELIMINATION'] as any)).toEqual([
      {
        rule: 'MATRIX',
        message: "Fase desconocida: 'CUSTOM'. No se puede validar la transición."
      }
    ]);
  });

  it('derives available stage options and consolation status from the current sequence', () => {
    expect(getAvailableStageOptions(['SINGLE_ELIMINATION'], 0)).toEqual([
      'SINGLE_ELIMINATION',
      'ROUND_ROBIN',
      'DOUBLE_ELIMINATION'
    ]);
    expect(getAvailableStageOptions(['SINGLE_ELIMINATION', 'CONSOLATION'], 1)).toEqual([
      'SINGLE_ELIMINATION',
      'CONSOLATION'
    ]);
    expect(getAvailableStageOptions(['CUSTOM' as never], 1)).toEqual([]);

    expect(isConsolationDisabled([])).toBeTrue();
    expect(isConsolationDisabled(['ROUND_ROBIN'])).toBeTrue();
    expect(isConsolationDisabled(['SINGLE_ELIMINATION'])).toBeFalse();
  });

  it('has paymentStatus field in TournamentInscriptionPlayer interface', () => {
    const player = {
      inscriptionId: 'ins-1',
      participantId: 'p-1',
      eventId: 'e-1',
      categoryId: 1,
      category: 'Absoluto',
      eventName: 'Absoluto Masculino',
      eventGender: 'MALE',
      firstName: 'Carlos',
      lastName: 'Lopez',
      gender: 'MALE',
      paymentStatus: 'PAID'
    };
    expect(player.paymentStatus).toBe('PAID');
  });

  it('has points field in TournamentRankingEntry interface', () => {
    const entry = {
      position: 1,
      participantId: 'p-1',
      personId: null,
      firstName: 'Carlos',
      lastName: 'Lopez',
      gender: 'MALE',
      points: 100
    };
    expect(entry.points).toBe(100);
  });
});
