/*
 * Copyright 2018-2020 Florian Spieß
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package club.minnced.discord.webhook;

import club.minnced.discord.webhook.send.AllowedMentions;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Builder for a {@link club.minnced.discord.webhook.WebhookClient} instance.
 *
 * @see club.minnced.discord.webhook.WebhookClient#create(long, String)
 */
public class WebhookClientBuilder { //TODO: tests
    protected final long id;
    protected final String token;
    protected ScheduledExecutorService pool;
    protected OkHttpClient client;
    protected ThreadFactory threadFactory;
    protected AllowedMentions allowedMentions = AllowedMentions.all();
    protected boolean isDaemon;
    protected boolean parseMessage = true;

    /**
     * Creates a new WebhookClientBuilder for the specified webhook components
     *
     * @param  id
     *         The webhook id
     *
     * @throws java.lang.NullPointerException
     *         If the token is null
     */
    public WebhookClientBuilder(final long id, final String token) {
        this.id = id;
        this.token = token;
    }

    /**
     * The {@link java.util.concurrent.ScheduledExecutorService} that is used to execute
     * send requests in the resulting {@link club.minnced.discord.webhook.WebhookClient}.
     * <br>This will be closed by a call to {@link WebhookClient#close()}.
     *
     * @param  executorService
     *         The executor service to use
     *
     * @return The current builder, for chaining convenience
     */
    @NotNull
    public WebhookClientBuilder setExecutorService(@Nullable ScheduledExecutorService executorService) {
        this.pool = executorService;
        return this;
    }

    /**
     * The {@link okhttp3.OkHttpClient} that is used to execute
     * send requests in the resulting {@link club.minnced.discord.webhook.WebhookClient}.
     * <br>It is usually not necessary to use multiple different clients in one application
     *
     * @param  client
     *         The http client to use
     *
     * @return The current builder, for chaining convenience
     */
    @NotNull
    public WebhookClientBuilder setHttpClient(@Nullable OkHttpClient client) {
        this.client = client;
        return this;
    }

    /**
     * The {@link java.util.concurrent.ThreadFactory} that is used to initialize
     * the default {@link java.util.concurrent.ScheduledExecutorService} used if
     * {@link #setExecutorService(java.util.concurrent.ScheduledExecutorService)} is not configured.
     *
     * @param  factory
     *         The factory to use
     *
     * @return The current builder, for chaining convenience
     */
    @NotNull
    public WebhookClientBuilder setThreadFactory(@Nullable ThreadFactory factory) {
        this.threadFactory = factory;
        return this;
    }

    /**
     * The default mention whitelist for every outgoing message.
     * <br>See {@link AllowedMentions} for more details.
     *
     * @param  mentions
     *         The mention whitelist
     *
     * @return This builder for chaining convenience
     */
    @NotNull
    public WebhookClientBuilder setAllowedMentions(@Nullable AllowedMentions mentions) {
        this.allowedMentions = mentions == null ? AllowedMentions.all() : mentions;
        return this;
    }

    /**
     * Whether the default executor should use daemon threads.
     * <br>This has no effect if either {@link #setExecutorService(java.util.concurrent.ScheduledExecutorService)}
     * or {@link #setThreadFactory(java.util.concurrent.ThreadFactory)} are configured to non-null values.
     *
     * @param  isDaemon
     *         Whether to use daemon threads or not
     *
     * @return The current builder, for chaining convenience
     */
    @NotNull
    public WebhookClientBuilder setDaemon(boolean isDaemon) {
        this.isDaemon = isDaemon;
        return this;
    }

    /**
     * Whether resulting messages should be parsed after sending,
     * if this is set to {@code false} the futures returned by {@link club.minnced.discord.webhook.WebhookClient}
     * will receive {@code null} instead of instances of {@link club.minnced.discord.webhook.receive.ReadonlyMessage}.
     *
     * @param  waitForMessage
     *         True, if the client should parse resulting messages (default behavior)
     *
     * @return The current builder, for chaining convenience
     */
    @NotNull
    public WebhookClientBuilder setWait(boolean waitForMessage) {
        this.parseMessage = waitForMessage;
        return this;
    }

    /**
     * Builds the {@link club.minnced.discord.webhook.WebhookClient}
     * with the current settings
     *
     * @return {@link club.minnced.discord.webhook.WebhookClient} instance
     */
    @NotNull
    public WebhookClient build() {
        OkHttpClient client = this.client == null ? new OkHttpClient() : this.client;
        ScheduledExecutorService pool = this.pool != null ? this.pool : getDefaultPool(id, threadFactory, isDaemon);
        return new WebhookClient(id, this.token, parseMessage, client, pool, allowedMentions);
    }

    protected static ScheduledExecutorService getDefaultPool(long id, ThreadFactory factory, boolean isDaemon) {
        return Executors.newSingleThreadScheduledExecutor(factory == null ? new DefaultWebhookThreadFactory(id, isDaemon) : factory);
    }

    private static final class DefaultWebhookThreadFactory implements ThreadFactory {
        private final long id;
        private final boolean isDaemon;

        public DefaultWebhookThreadFactory(long id, boolean isDaemon) {
            this.id = id;
            this.isDaemon = isDaemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread thread = new Thread(r, "Webhook-RateLimit Thread WebhookID: " + id);
            thread.setDaemon(isDaemon);
            return thread;
        }
    }
}
