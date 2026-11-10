import { TestBed } from '@angular/core/testing';
import { OfflineStorageService } from './offline-storage.service';

describe('OfflineStorageService', () => {
  let service: OfflineStorageService;

  beforeEach(async () => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(OfflineStorageService);
    await service.clear();
  });

  afterEach(async () => {
    await service.clear();
  });

  it('should store and read a record with its cached timestamp', async () => {
    await service.write('tournament:123', { id: '123', name: 'Open de prueba' });

    const record = await service.read<{ id: string; name: string }>('tournament:123');

    expect(record).not.toBeNull();
    expect(record!.data.name).toBe('Open de prueba');
    expect(Number.isNaN(Date.parse(record!.cachedAt))).toBeFalse();
  });

  it('should return null for unknown keys', async () => {
    expect(await service.read('missing')).toBeNull();
  });

  it('should remove a single record and clear every record', async () => {
    await service.write('first', { value: 1 });
    await service.write('second', { value: 2 });

    await service.remove('first');
    expect(await service.read('first')).toBeNull();
    expect(await service.read('second')).not.toBeNull();

    await service.clear();
    expect(await service.read('second')).toBeNull();
  });
});
