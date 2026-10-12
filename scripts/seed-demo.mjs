#!/usr/bin/env node
/**
 * Seeds a realistic Noba demo dataset so you can sign in and exercise the app.
 *
 * - Accounts + providers + services + business hours are created through the real API (invariants enforced).
 * - Provider activation, catalog extras, payments, loyalty, gift cards, referrals, appointments, reviews,
 *   live queues and waitlists are written with schema-accurate SQL.
 *
 * Requirements:
 *   - Backend running with OTP codes captured to a log file (dev uses the LogTestSmsProvider).
 *       ./mvnw spring-boot:run > /tmp/noba-backend.log 2>&1 &
 *   - Postgres reachable via `docker exec -i noba-postgres-dev psql ...` (docker-compose.dev.yml).
 *
 * Usage:
 *   BACKEND_LOG=/tmp/noba-backend.log node scripts/seed-demo.mjs
 *   BACKEND_LOG=/tmp/noba-backend.log node scripts/seed-demo.mjs --reset   # wipe app tables first
 */
import { readFileSync, statSync } from 'node:fs';
import { spawnSync } from 'node:child_process';

const API = process.env.API_URL ?? 'http://localhost:8080/api/v1';
const LOG = process.env.BACKEND_LOG ?? '/tmp/noba-backend.log';
const PG_CONTAINER = process.env.PG_CONTAINER ?? 'noba-postgres-dev';
const RESET = process.argv.includes('--reset');

const sleep = ms => new Promise(r => setTimeout(r, ms));
const q = s => `'${String(s).replace(/'/g, "''")}'`;
const uuid = () => crypto.randomUUID();

async function post(path, body, token) {
  const res = await fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) throw new Error(`POST ${path} -> ${res.status} ${text.slice(0, 300)}`);
  return data;
}
async function put(path, body, token) {
  const res = await fetch(API + path, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`PUT ${path} -> ${res.status} ${text.slice(0, 300)}`);
  return text ? JSON.parse(text) : null;
}
async function get(path, token) {
  const res = await fetch(API + path, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  const text = await res.text();
  if (!res.ok) throw new Error(`GET ${path} -> ${res.status} ${text.slice(0, 300)}`);
  return text ? JSON.parse(text) : null;
}

function sql(statement, { quiet = true } = {}) {
  const args = ['exec', '-i', PG_CONTAINER, 'psql', '-U', 'postgres', '-d', 'noba', '-v', 'ON_ERROR_STOP=1', '-f', '-'];
  const res = spawnSync('docker', args, { input: statement, encoding: 'utf8' });
  if (res.status !== 0) throw new Error(`psql failed:\n${res.stderr?.slice(0, 800)}\n---\n${statement.slice(0, 800)}`);
  if (!quiet && res.stdout) process.stdout.write(res.stdout);
  return res.stdout ?? '';
}

function scalar(statement) {
  const res = spawnSync('docker',
    ['exec', '-i', PG_CONTAINER, 'psql', '-U', 'postgres', '-d', 'noba', '-t', '-A', '-c', statement],
    { encoding: 'utf8' });
  if (res.status !== 0) throw new Error(`psql scalar failed: ${res.stderr?.slice(0, 400)}`);
  return (res.stdout ?? '').trim();
}

function readOtp(fromOffset) {
  const text = readFileSync(LOG).subarray(fromOffset).toString('utf8');
  const matches = [...text.matchAll(/verification code:\s*(\d+)/g)];
  return matches.length ? matches[matches.length - 1][1] : null;
}

/** Clears the per-IP auth rate-limit key so seeding many accounts doesn't trip it. */
function clearAuthIpLimit() {
  const container = process.env.REDIS_CONTAINER ?? 'noba-redis-dev';
  const patterns = ['rate_limit:rl:ip:*', 'ratelimit:*:/api/v1/auth*'];
  for (const pattern of patterns) {
    try {
      const scan = spawnSync('docker', ['exec', container, 'redis-cli', '--scan', '--pattern', pattern], { encoding: 'utf8' });
      const keys = (scan.stdout ?? '').trim().split('\n').filter(Boolean);
      if (keys.length) spawnSync('docker', ['exec', container, 'redis-cli', 'DEL', ...keys], { encoding: 'utf8' });
    } catch {
      // best-effort
    }
  }
}

/** OTP login (dev logs the code). Returns an access token. */
async function login(phone) {
  clearAuthIpLimit();
  const from = statSync(LOG).size;
  const challenge = await post('/auth/otp/start', { target: phone, channel: 'SMS', purpose: 'LOGIN' });
  let code = null;
  for (let i = 0; i < 25 && !code; i++) {
    await sleep(200);
    code = readOtp(from);
  }
  if (!code) throw new Error(`No OTP found in ${LOG}. Start the backend with: ./mvnw spring-boot:run > ${LOG} 2>&1 &`);
  const token = await post('/auth/otp/verify', { challengeId: challenge.challengeId, code });
  return token.accessToken;
}

const ME = async token => get('/users/me', token);

// ── Demo data model ──────────────────────────────────────────────────────────

const PROVIDERS = [
  {
    phone: '+212700000001', name: 'Atlas Barber Club', category: 'Barber', city: 'Casablanca',
    lat: 33.5899, lng: -7.6039, tier: 'PREMIUM', accessible: { wheelchair: true },
    desc: 'Barber premium au cœur de Casablanca : coupes, barbes et rasage traditionnel.',
    services: [
      { name: 'Coupe classique', minutes: 45, price: 60 },
      { name: 'Coupe + Barbe', minutes: 60, price: 90 },
      { name: 'Rasage traditionnel', minutes: 30, price: 50 },
      { name: 'Coloration', minutes: 75, price: 150 },
    ],
    ratings: [5, 5, 4, 5, 4, 5],
  },
  {
    phone: '+212700000002', name: 'Salon Lumière', category: 'Salon', city: 'Casablanca',
    lat: 33.5883, lng: -7.6114, tier: 'BASIC', accessible: { sensory: true },
    desc: 'Salon de coiffure et soins capillaires, équipe expérimentée.',
    services: [
      { name: 'Brushing', minutes: 40, price: 80 },
      { name: 'Coupe femme', minutes: 60, price: 150 },
      { name: 'Coloration', minutes: 120, price: 350 },
      { name: 'Soin profond', minutes: 45, price: 120 },
    ],
    ratings: [4, 5, 4, 4, 5],
  },
  {
    phone: '+212700000003', name: 'Coiffure El Amrani', category: 'Barber', city: 'Rabat',
    lat: 34.0209, lng: -6.8416, tier: 'FREE',
    desc: 'Le rendez-vous des hommes à Rabat. Coupe soignée et ambiance conviviale.',
    services: [
      { name: 'Coupe homme', minutes: 30, price: 50 },
      { name: 'Taille de barbe', minutes: 20, price: 30 },
    ],
    ratings: [4, 4, 5],
  },
  {
    phone: '+212700000004', name: 'Clinique Dr. Benali', category: 'Doctor', city: 'Casablanca',
    lat: 33.5731, lng: -7.5898, tier: 'ENTERPRISE', accessible: { wheelchair: true, sign: true },
    desc: 'Médecine générale et bilans de santé sur rendez-vous.',
    services: [
      { name: 'Consultation générale', minutes: 30, price: 200 },
      { name: 'Bilan de santé', minutes: 60, price: 400 },
    ],
    ratings: [5, 5, 5, 4],
  },
  {
    phone: '+212700000005', name: 'Spa Oasis', category: 'Spa', city: 'Marrakech',
    lat: 31.6295, lng: -7.9811, tier: 'PREMIUM', accessible: { wheelchair: true, sensory: true },
    desc: 'Hammam, massages et soins du visage dans un cadre paisible.',
    closedToday: true,
    services: [
      { name: 'Massage relaxant', minutes: 60, price: 300 },
      { name: 'Hammam', minutes: 45, price: 180 },
      { name: 'Soin du visage', minutes: 60, price: 250 },
    ],
    ratings: [5, 4, 5, 5],
  },
  {
    phone: '+212700000006', name: 'Dar Tajine', category: 'Restaurant', city: 'Casablanca',
    lat: 33.5992, lng: -7.6181, tier: 'BASIC',
    desc: 'Cuisine marocaine authentique. Réservation de table conseillée.',
    services: [{ name: 'Menu midi', minutes: 60, price: 80 }],
    ratings: [4, 5, 4, 5, 4],
  },
  {
    phone: '+212700000007', name: 'AutoPro Mécanique', category: 'Mechanic', city: 'Casablanca',
    lat: 33.5651, lng: -7.6051, tier: 'FREE',
    desc: 'Entretien et diagnostic automobile, devis rapide.',
    services: [
      { name: 'Vidange', minutes: 30, price: 250 },
      { name: 'Diagnostic électronique', minutes: 45, price: 150 },
    ],
    ratings: [4, 4, 3, 5],
  },
  {
    phone: '+212700000008', name: 'Fitness Club Atlas', category: 'Fitness', city: 'Rabat',
    lat: 33.9716, lng: -6.8498, tier: 'BASIC', accessible: { wheelchair: true },
    desc: 'Salle de sport moderne, coaching personnalisé et cours collectifs.',
    services: [
      { name: 'Séance libre', minutes: 60, price: 100 },
      { name: 'Coaching personnel', minutes: 60, price: 250 },
    ],
    ratings: [4, 5, 4],
  },
];

const CLIENTS = [
  { phone: '+212700000010', first: 'Amine', last: 'Tazi', demo: true },
  { phone: '+212700000011', first: 'Sara', last: 'Bennani' },
  { phone: '+212700000012', first: 'Youssef', last: 'Alaoui' },
  { phone: '+212700000013', first: 'Fatima', last: 'Zahra' },
  { phone: '+212700000014', first: 'Omar', last: 'Idrissi' },
  { phone: '+212700000015', first: 'Leila', last: 'Chraibi' },
];

function todayJavaDow() {
  const d = new Date().getDay(); // 0=Sun..6=Sat
  return d === 0 ? 7 : d;
}

// ── Run ──────────────────────────────────────────────────────────────────────

async function main() {
  if (RESET) {
    console.log('• --reset: truncating application tables…');
    sql(`DO $$ DECLARE r RECORD; BEGIN
      FOR r IN SELECT tablename FROM pg_tables WHERE schemaname='public'
               AND tablename NOT LIKE 'qrtz_%'
               AND tablename NOT IN ('flyway_schema_history', 'role', 'service_category', 'otp_templates') LOOP
        EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
      END LOOP; END $$;`);
  }

  if (!RESET && scalar(`SELECT count(*) FROM users WHERE primary_phone = '+212700000010';`).trim() !== '0') {
    console.log('⚠ Demo data already present (phone +212700000010). Re-run with --reset for a clean slate.');
    process.exit(0);
  }

  // 1) Providers + hours (API)
  const providers = [];
  for (const p of PROVIDERS) {
    process.stdout.write(`• provider ${p.name}\n`);
    const token = await login(p.phone);
    let created;
    try {
      created = await get('/providers/self', token);
    } catch {
      created = await post('/providers/self', {
        name: p.name, description: p.desc, category: p.category,
        address: `${p.city}`, latitude: p.lat, longitude: p.lng,
        firstName: p.name.split(' ')[0], lastName: 'Owner',
      }, token);
    }
    const dow = todayJavaDow();
    const days = [1, 2, 3, 4, 5, 6, 7].map(d => ({
      dayOfWeek: d,
      opensAt: '00:00',
      closesAt: '23:59',
      closed: p.closedToday === true && d === dow,
    }));
    await put('/providers/self/hours', { days }, token);
    providers.push({ ...p, id: created.id, token });
  }

  // 2) Clients + user ids
  const clients = [];
  for (const c of CLIENTS) {
    process.stdout.write(`• client ${c.first} ${c.last}\n`);
    const token = await login(c.phone);
    const me = await ME(token);
    clients.push({ ...c, id: me.id, token });
  }
  const demo = clients.find(c => c.demo);

  // 3) Catalog extras + activation (SQL)
  const p = Object.fromEntries(providers.map(x => [x.name, x]));
  const c = Object.fromEntries(clients.map(x => [x.phone, x]));

  const s = [];
  s.push('BEGIN;');

  // activate + enrich providers
  for (const prov of providers) {
    s.push(`UPDATE provider SET status='ACTIVE', verified=true, verification_date=now(),
            wheelchair_accessible=${prov.accessible?.wheelchair ? 'true' : 'false'},
            sign_language=${prov.accessible?.sign ? 'true' : 'false'},
            sensory_friendly=${prov.accessible?.sensory ? 'true' : 'false'}
          WHERE id=${q(prov.id)};`);
    // services (written directly: the per-tier API limit would cap these)
    s.push(`DELETE FROM provider_service WHERE provider_id=${q(prov.id)};`);
    for (const svc of prov.services) {
      s.push(`INSERT INTO provider_service(provider_id, name, duration, price, active, category, created_at)
              VALUES (${q(prov.id)}, ${q(svc.name)}, interval '${svc.minutes} minutes', ${svc.price}, true, ${q(prov.category)}, now());`);
    }
    s.push(`INSERT INTO provider_settings(provider_id, notification_locale) VALUES (${q(prov.id)}, 'fr') ON CONFLICT DO NOTHING;`);
    // subscription (the app may auto-create a FREE one)
    const tier = prov.tier ?? 'FREE';
    s.push(`INSERT INTO provider_subscription(id, provider_id, tier, status, started_at, created_at, updated_at)
            VALUES (gen_random_uuid(), ${q(prov.id)}, ${q(tier)}, 'ACTIVE', now() - interval '10 days', now(), now())
            ON CONFLICT (provider_id) DO UPDATE SET tier=EXCLUDED.tier, status='ACTIVE', updated_at=now();`);
    // payout settings
    s.push(`INSERT INTO provider_payout_settings(provider_id, stripe_account_id, payouts_enabled)
            VALUES (${q(prov.id)}, ${q('acct_demo_' + prov.id.slice(0, 8))}, true) ON CONFLICT DO NOTHING;`);
    // locations
    s.push(`INSERT INTO provider_location(provider_id, name, address, latitude, longitude)
            VALUES (${q(prov.id)}, ${q(prov.name + ' — Centre')}, ${q(prov.city)}, ${prov.lat}, ${prov.lng});`);
    // employees
    s.push(`INSERT INTO provider_employee(provider_id, name, role)
            VALUES (${q(prov.id)}, ${q('Yassine')}, 'Senior'), (${q(prov.id)}, ${q('Nadia')}, 'Assistant');`);
    // packages
    s.push(`INSERT INTO service_package(provider_id, name, description, price, original_price, valid_days)
            VALUES (${q(prov.id)}, ${q('Pack 3 visites')}, ${q('Trois visites au tarif préférentiel')},
                    round(${q((prov.services.reduce((a, x) => a + x.price, 0) * 0.85).toFixed(2))}::numeric, 2),
                    ${q(prov.services.reduce((a, x) => a + x.price, 0).toFixed(2))}, 60);`);
    // announcement
    s.push(`INSERT INTO announcement(provider_id, title, message, important)
            VALUES (${q(prov.id)}, ${q('Bienvenue chez ' + prov.name)}, ${q('Nouvelle offre de bienvenue cette semaine !')}, false);`);
    // portfolio media
    s.push(`INSERT INTO provider_media(provider_id, url, caption, media_order)
            VALUES (${q(prov.id)}, ${q('https://picsum.photos/seed/' + prov.id.slice(0, 6) + '/800/600')}, ${q('Notre espace')}, 0);`);
    // loyalty rewards
    s.push(`INSERT INTO loyalty_reward(id, provider_id, name, description, points_required, is_active)
            VALUES (gen_random_uuid(), ${q(prov.id)}, ${q('Réduction 20%')}, ${q('20% sur votre prochaine visite')}, 100, true),
                   (gen_random_uuid(), ${q(prov.id)}, ${q('Service offert')}, ${q('Une prestation offerte')}, 250, true);`);
    // promotions (client-visible) — one per provider
    const promoId = uuid();
    s.push(`INSERT INTO promotion(id, provider_id, title, description, discount_percent, valid_from, valid_to, is_active)
            VALUES (${q(promoId)}, ${q(prov.id)}, ${q('-15% cette semaine')}, ${q('Offre limitée')}, 15,
                    now() - interval '1 day', now() + interval '30 days', true);`);
  }

  // reviews (distinct customers per provider, value 3..5)
  for (const prov of providers) {
    const reviewers = clients.slice(0, 4);
    reviewers.forEach((cl, i) => {
      const value = Math.min(5, 3 + ((i + prov.name.length) % 3));
      s.push(`INSERT INTO provider_rating(provider_id, customer_id, value, comment, created_at, updated_at)
              VALUES (${q(prov.id)}, ${q(cl.id)}, ${value}, ${q('Très bon service, je recommande.')}, now() - interval '${i + 2} days', now());`);
    });
  }

  // favorites for the demo client
  for (const prov of providers.slice(0, 3)) {
    s.push(`INSERT INTO provider_favorite(provider_id, customer_id, created_at)
            VALUES (${q(prov.id)}, ${q(demo.id)}, now());`);
  }

  // appointments (upcoming + past) for the demo client
  const apptProvider = p['Atlas Barber Club'];
  s.push(`INSERT INTO appointment(user_id, provider_id, service_id, start_time, end_time, status, notes, created_at, updated_at)
          VALUES (${q(demo.id)}, ${q(apptProvider.id)}, NULL, now() + interval '2 days', now() + interval '2 days 45 minutes', 'CONFIRMED', ${q('Coupe + barbe')}, now(), now()),
                 (${q(demo.id)}, ${q(apptProvider.id)}, NULL, now() - interval '6 days', now() - interval '6 days' + interval '45 minutes', 'COMPLETED', NULL, now() - interval '7 days', now() - interval '7 days');`);

  // payments (statement) for the demo client
  const payProvider = p['Salon Lumière'];
  s.push(`INSERT INTO payment_transaction(id, user_id, provider_id, currency, amount, fee, net_amount, status, stripe_payment_intent_id, completed_at)
          VALUES (gen_random_uuid(), ${q(demo.id)}, ${q(payProvider.id)}, 'MAD', 150, 7.5, 142.5, 'SUCCEEDED', ${q('pi_demo_' + uuid().slice(0, 12))}, now() - interval '3 days'),
                 (gen_random_uuid(), ${q(demo.id)}, ${q(apptProvider.id)}, 'MAD', 90, 4.5, 85.5, 'SUCCEEDED', ${q('pi_demo_' + uuid().slice(0, 12))}, now() - interval '10 days'),
                 (gen_random_uuid(), ${q(demo.id)}, ${q(apptProvider.id)}, 'MAD', 60, 3, 57, 'FAILED', NULL, NULL);`);

  // loyalty balances + history for the demo client
  s.push(`INSERT INTO loyalty_points(id, user_id, provider_id, total_points, created_at, updated_at)
          VALUES (gen_random_uuid(), ${q(demo.id)}, ${q(apptProvider.id)}, 120, now(), now()),
                 (gen_random_uuid(), ${q(demo.id)}, ${q(payProvider.id)}, 60, now(), now());`);
  s.push(`INSERT INTO loyalty_points_transaction(id, user_id, provider_id, points, reason, created_at)
          VALUES (gen_random_uuid(), ${q(demo.id)}, ${q(apptProvider.id)}, 90, 'SERVICE_COMPLETED', now() - interval '20 days'),
                 (gen_random_uuid(), ${q(demo.id)}, ${q(apptProvider.id)}, 50, 'SERVICE_COMPLETED', now() - interval '10 days'),
                 (gen_random_uuid(), ${q(demo.id)}, ${q(apptProvider.id)}, -20, 'REDEEMED:Réduction 20%', now() - interval '5 days'),
                 (gen_random_uuid(), ${q(demo.id)}, ${q(payProvider.id)}, 60, 'SERVICE_COMPLETED', now() - interval '3 days');`);

  // gift cards for the demo client (one active, one redeemed)
  s.push(`INSERT INTO gift_card(provider_id, buyer_id, amount, code, message, redeemed)
          VALUES (${q(apptProvider.id)}, ${q(demo.id)}, 100, ${q('NOBA-' + uuid().slice(0, 8).toUpperCase())}, ${q('Joyeux anniversaire !')}, false),
                 (${q(payProvider.id)}, ${q(c['+212700000011'].id)}, 50, ${q('NOBA-' + uuid().slice(0, 8).toUpperCase())}, NULL, true);`);

  // referrals for the demo client
  s.push(`INSERT INTO referral(id, referrer_id, referee_id, code, status, reward_claimed, reward_type, created_at, updated_at)
          VALUES (gen_random_uuid(), ${q(demo.id)}, ${q(c['+212700000012'].id)}, ${q('AMINE-' + uuid().slice(0, 5).toUpperCase())}, 'COMPLETED', true, 'PRIORITY_PASS', now() - interval '15 days', now() - interval '12 days'),
                 (gen_random_uuid(), ${q(demo.id)}, NULL, ${q('AMINE-' + uuid().slice(0, 5).toUpperCase())}, 'PENDING', false, NULL, now() - interval '2 days', now() - interval '2 days');`);

  // provider analytics snapshots (last 14 days)
  const anProvider = p['Atlas Barber Club'];
  for (let i = 0; i < 14; i++) {
    s.push(`INSERT INTO queue_analytics_snapshot(provider_id, date, total_served, avg_wait_time_minutes, avg_service_duration_minutes, peak_hour)
            VALUES (${q(anProvider.id)}, (now() - interval '${i} days')::date, ${8 + (i % 6)}, ${10 + (i % 8)}, ${35 + (i % 15)}, ${10 + (i % 8)});`);
  }

  // a couple of waitlist entries at the closed Spa
  const spa = p['Spa Oasis'];
  s.push(`INSERT INTO queue_waitlist(provider_id, client_id, status, created_at, updated_at)
          VALUES (${q(spa.id)}, ${q(demo.id)}, 'WAITING', now() - interval '20 minutes', now() - interval '20 minutes'),
                 (${q(spa.id)}, ${q(c['+212700000013'].id)}, 'WAITING', now() - interval '10 minutes', now() - interval '10 minutes');`);

  s.push('COMMIT;');
  console.log('• writing catalog/payments/loyalty/… to the database');
  sql(s.join('\n'));

  // 4) Live queues (API joins give correct positions + estimates), then promote the first to ACTIVE
  const queueProviders = [p['Atlas Barber Club'], p['Salon Lumière']];
  const queueClients = clients.slice(0, 3);
  for (const prov of queueProviders) {
    for (const cl of queueClients) {
      try {
        await post(`/queues/providers/${prov.id}/join`, undefined, cl.token);
      } catch (e) {
        console.warn(`  (join skipped: ${e.message.slice(0, 80)})`);
      }
    }
    try {
      await post('/queues/providers/self/next', undefined, prov.token); // position 1 -> ACTIVE
    } catch (e) {
      console.warn(`  (advance skipped: ${e.message.slice(0, 80)})`);
    }
  }

  console.log('\n✅ Demo data seeded.\n');
  console.log('   Client login (rich data):  +212700000010  (Amine)');
  console.log('   Other clients:             +212700000011 … +212700000015');
  console.log('   Provider logins:           +212700000001 … +212700000008');
  console.log('   Closed provider (waitlist):Spa Oasis (+212700000005)');
  console.log(`   OTP codes are logged to:   ${LOG}\n`);
}

main().catch(err => {
  console.error('\n❌ Seeding failed:', err.message);
  process.exit(1);
});
