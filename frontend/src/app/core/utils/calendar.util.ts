export interface CalendarEventInfo {
  title: string;
  startTime: string;
  endTime: string;
}

// Google Calendar dátumformátum: alap ISO 8601, kötőjelek/kettőspontok és a
// tört másodperc nélkül (pl. 20260722T110000Z).
function toCompactUtc(iso: string): string {
  return new Date(iso).toISOString().replace(/[-:]/g, '').split('.')[0] + 'Z';
}

export function buildGoogleCalendarUrl(event: CalendarEventInfo): string {
  const params = new URLSearchParams({
    action: 'TEMPLATE',
    text: event.title,
    dates: `${toCompactUtc(event.startTime)}/${toCompactUtc(event.endTime)}`,
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

// ICS mezőkben a vessző/pontosvessző/sortörés escape-elendő - a címünkben
// nincs ilyen karakter reálisan, de defenzíven megcsináljuk.
function escapeIcsText(value: string): string {
  return value.replace(/([,;])/g, '\\$1').replace(/\n/g, '\\n');
}

export function downloadIcsFile(event: CalendarEventInfo): void {
  const uid = `${crypto.randomUUID()}@booking-system`;
  const lines = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//Booking System//EN',
    'BEGIN:VEVENT',
    `UID:${uid}`,
    `DTSTAMP:${toCompactUtc(new Date().toISOString())}`,
    `DTSTART:${toCompactUtc(event.startTime)}`,
    `DTEND:${toCompactUtc(event.endTime)}`,
    `SUMMARY:${escapeIcsText(event.title)}`,
    'END:VEVENT',
    'END:VCALENDAR',
  ];

  const blob = new Blob([lines.join('\r\n')], { type: 'text/calendar' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = 'booking.ics';
  link.click();
  URL.revokeObjectURL(url);
}
