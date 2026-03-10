import { render, screen, waitFor } from '@testing-library/react';
import App from './App';

beforeEach(() => {
  global.fetch = jest.fn((url) => {
    if (String(url).includes('/reference/rules')) {
      return Promise.resolve({
        ok: true,
        text: () => Promise.resolve(JSON.stringify({
          initialSetup: { aircraftCount: 3 },
          missions: [
            { code: 'M1', name: 'Recon', flightHours: 4, fuelCost: 20, weaponCost: 0 },
            { code: 'M2', name: 'Strike', flightHours: 6, fuelCost: 30, weaponCost: 2 },
            { code: 'M3', name: 'Deep Strike', flightHours: 8, fuelCost: 40, weaponCost: 4 },
          ],
        })),
      });
    }

    return Promise.resolve({
      ok: true,
      text: () => Promise.resolve('{}'),
    });
  });
});

test('renders smart air base shell', async () => {
  render(<App />);
  await waitFor(() => {
    expect(global.fetch).toHaveBeenCalled();
  });
  expect(screen.getByText(/Smart Air Base/i)).toBeInTheDocument();
});

test('loads mission cards from rules endpoint', async () => {
  render(<App />);

  await waitFor(() => {
    expect(screen.getByText(/M1-1 - Recon/i)).toBeInTheDocument();
  });
});
