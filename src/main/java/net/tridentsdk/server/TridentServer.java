/*
 * Copyright (c) 2014, The TridentSDK Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     1. Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the The TridentSDK Team nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL The TridentSDK Team BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.tridentsdk.server;

import net.tridentsdk.api.Server;
import net.tridentsdk.api.Trident;
import net.tridentsdk.server.netty.protocol.Protocol;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The access base to internal workings of the server
 *
 * @author The TridentSDK Team
 */
@ThreadSafe
public final class TridentServer implements Server, Runnable {
    private final AtomicReference<Thread> SERVER_THREAD = new AtomicReference<>();
    //private final ProfileRepository PROFILE_REPOSITORY = new HttpProfileRepository("minecraft");

    private final TridentConfig config;
    private final Protocol protocol;
    private final TransferQueue<Runnable> threadTasks = new LinkedTransferQueue<>();

    private boolean stopped;

    private TridentServer(TridentConfig config) {
        this.config = config;
        this.protocol = new Protocol();
    }

    /**
     * Creates the server access base, distributing information to the fields available
     *
     * @param config the configuration to use for option lookup
     */
    public static TridentServer createServer(TridentConfig config) {
        TridentServer server = new TridentServer(config);
        Trident.setServer(server);

        server.SERVER_THREAD.set(new Thread(server, "TridentServer Main Thread"));
        server.SERVER_THREAD.get().start();

        return server;
        // We CANNOT let the "this" instance escape during creation, else we lose thread-safety
    }

    /**
     * Get the protocol base of the server
     *
     * @return the access to server protocol
     */
    public Protocol getProtocol() {
        return this.protocol;
    }

    /*
    public ProfileRepository getProfileRepository() {
        return this.PROFILE_REPOSITORY;
    } */

    /**
     * Gets the port the server currently runs on
     *
     * @return the port occupied by the server
     */
    @Override
    public int getPort() {
        return (int) this.config.getPort();
    }

    /**
     * Puts a task into the execution queue
     */
    public void addTask(Runnable task) {
        this.threadTasks.add(task);
    }

    @Override
    public void run() {
        //TODO: Set some server stuff up

        //TODO: Main server Loop
        while (!this.stopped) {
            try {
                // task cannot be null because it will block until there is an
                // element in the linked queue
                Runnable task = this.threadTasks.take();
                task.run();
            } catch (InterruptedException ignored) {
            }
            this.run();
        }
    }

    /**
     * Performs the shutdown procedure on the server, ending with the exit of the JVM
     */
    @Override
    public void shutdown() {
        //TODO: Cleanup stuff...
        this.SERVER_THREAD.get().interrupt();
        try {
            this.threadTasks.transfer(new Runnable() {
                @Override public void run() {
                    TridentServer.this.stopped = true;
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
