package com.mrmccormick.hydra.mqtt.implementation.actor;

import com.mrmccormick.hydra.mqtt.GatewayHook;
import com.mrmccormick.hydra.mqtt.domain.Event;
import com.mrmccormick.hydra.mqtt.domain.actor.*;
import com.mrmccormick.hydra.mqtt.implementation.ChunkBuffer;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class BufferActor extends ActorBase implements IActor, IRunnable {

    public BufferActor(@NotNull String connectionName,
                       @NotNull String name,
                       @NotNull IRunnerBuilder runnerBuilder) {
        super(name);
        //noinspection ConstantValue
        if (connectionName == null)
            throw new IllegalArgumentException("connectionName can not be null");

        //noinspection ConstantValue
        if (runnerBuilder == null)
            throw new IllegalArgumentException("runnerBuilder can not be null");
        _runner = runnerBuilder.build(this);

        var logName = connectionName + "." + getClass().getSimpleName() + "." + name;
        _logger = GatewayHook.getLogger(logName);
        _chunkBuffer = new ChunkBuffer<>(logName + ".Buffer");
    }

    @Override
    public boolean clear() {
        _clearFlag = true;
        for (int i = 0; i < 1000; i++) {
            if (_clearAcknowledged) {
                _clearFlag = false;
                _clearAcknowledged = false;
                return true;
            }

            try {
                Thread.sleep(10);
            } catch (Exception e) {
                _logger.warn("Failed to sleep while waiting for buffer to clear: " + e, e);
                break;
            }
        }

        _clearFlag = false;
        _clearAcknowledged = false;
        return false;
    }

    @Override
    public IRunner getRunner() {
        return _runner;
    }

    @Override
    public void receive(@NotNull Event event) {
        _chunkBuffer.addToNextChunk(event);
    }

    @Override
    public void run() {
        int i = -1;
        do {
            i++;

            if (_chunkBuffer.currentChunkIsEmpty())
                _chunkBuffer.loadNextChunk(i);

            while (!_chunkBuffer.currentChunkIsEmpty()) {
                Event event = _chunkBuffer.currentChunkDequeue();

                if (_clearFlag)
                    break;

                for (IActor subscriber : _subscribers) {
                    try {
                        // Throwing an exception might kill the thread.
                        subscriber.receive(event);
                    } catch (Exception e) {
                        // This might spam the log.
                        _logger.warn("Failed to publish event to " + subscriber.getName() + ": " + e, e);
                    }
                }
            }

            if (_clearFlag) {
                _chunkBuffer.clear();
                _clearAcknowledged = true;
                return;
            }

        } while (!_chunkBuffer.nextChunkIsEmpty());
    }

    private final @NotNull ChunkBuffer<Event> _chunkBuffer;
    private volatile boolean _clearAcknowledged = false;
    private volatile boolean _clearFlag = false;
    private final @NotNull Logger _logger;
    private final IRunner _runner;
}
