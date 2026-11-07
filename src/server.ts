import { createApp } from './app.js';
import { env } from './config.js';
import { getRabbitChannel } from './helpers/rabbit.js';

async function main() {
  await getRabbitChannel();
  const app = createApp();
  app.listen(env.PORT, () => {
    console.log(`API listening on http://localhost:${env.PORT}`);
  });
}

main().catch((err) => {
  console.error('Failed to start API:', err);
  process.exit(1);
});
