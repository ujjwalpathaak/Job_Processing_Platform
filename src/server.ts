import { createApp } from './app.js';
import { env } from './config.js';
import { getRabbitChannel } from './helpers/rabbit.js';
import { log } from './services/logService.js';

async function main() {
  await getRabbitChannel();
  const app = createApp();
  app.listen(env.PORT, () => {
    log.info('event=api.started port={}', env.PORT);
  });
}

main().catch((err) => {
  log.error('event=api.start_failed reason={}', err instanceof Error ? err.message : err);
  process.exit(1);
});
