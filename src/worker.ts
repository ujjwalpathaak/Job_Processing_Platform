import { startCategoryConsumers, startRetryRouter } from './consumers/jobConsumer.js';
import { startLogRagConsumer } from './consumers/logRagConsumer.js';
import { getRabbitChannel } from './helpers/rabbit.js';

async function main() {
  await getRabbitChannel();
  await startCategoryConsumers();
  await startRetryRouter();
  await startLogRagConsumer();
  console.log('Worker consumers started');
}

main().catch((err) => {
  console.error('Failed to start worker:', err);
  process.exit(1);
});
