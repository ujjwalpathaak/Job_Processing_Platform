import { startCategoryConsumers, startRetryRouter } from './consumers/jobConsumer.js';
import { startLogRagConsumer } from './consumers/logRagConsumer.js';
import { getRabbitChannel } from './helpers/rabbit.js';
import { log } from './services/logService.js';

async function main() {
  await getRabbitChannel();
  await startCategoryConsumers();
  await startRetryRouter();
  await startLogRagConsumer();
  log.info('event=worker.started');
}

main().catch((err) => {
  log.error('event=worker.start_failed reason={}', err instanceof Error ? err.message : err);
  process.exit(1);
});
