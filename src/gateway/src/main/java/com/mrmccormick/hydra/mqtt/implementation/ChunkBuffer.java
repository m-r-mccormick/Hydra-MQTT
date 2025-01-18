package com.mrmccormick.hydra.mqtt.implementation;

import com.mrmccormick.hydra.mqtt.GatewayHook;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;

public class ChunkBuffer<T> {
    public ChunkBuffer(@NotNull String name) {
        //noinspection ConstantValue
        if (name == null)
            throw new NullPointerException("name can not be null");

        _logger = GatewayHook.getLogger(name);
    }

    public void addToNextChunk(@NotNull T object) {
        //noinspection ConstantValue
        if (object == null)
            throw new IllegalArgumentException("object can not be null");

        synchronized (_nextChunkLock) {
            _nextChunk.add(object);
        }
    }

    public void clear() {
        synchronized (_nextChunkLock) {
            _nextChunk.clear();
        }
        _currentChunk.clear();
    }

    public T currentChunkDequeue() {
        return _currentChunk.removeFirst();
    }

    public boolean currentChunkIsEmpty() {
        return _currentChunk.isEmpty();
    }

    public void loadNextChunk(@Nullable Integer loopIteration) {
        String loopId = "";
        if (loopIteration != null)
            loopId = " [loop i=" + loopIteration + "]";

        synchronized (_nextChunkLock) {
            _currentChunk = _nextChunk;
            _nextChunk = new LinkedList<>();
        }

        int sizeWarningThreshold = 100_000;
        int sizeSevereWarningThreshold = 1_000_000;
        if (_currentChunk.size() > sizeWarningThreshold && Instant.now().isAfter(_nextSizeWarning)) {
            if (_currentChunk.size() > sizeSevereWarningThreshold) {
                _nextSizeWarning = Instant.now().plus(_sizeWarningInterval);
                _logger.warn("Buffer is severely overloaded: " + String.format("%,d", _currentChunk.size()) +
                        " events in the current chunk." + loopId);
            } else {
                _nextSizeWarning = Instant.now().plus(_sizeWarningInterval);
                _logger.warn("Buffer is overloaded: " + String.format("%,d", _currentChunk.size()) +
                        " events in the current chunk." + loopId);
            }
            _warningTriggered = true;
        } else {
            if (_warningTriggered) {
                _warningTriggered = false;
                _logger.info("Buffer is no longer overloaded." + loopId);
            }
        }
    }

    public boolean nextChunkIsEmpty() {
        synchronized (_nextChunkLock) {
            return _nextChunk.isEmpty();
        }
    }

    private @NotNull LinkedList<T> _currentChunk = new LinkedList<>();
    private final @NotNull Logger _logger;
    private @NotNull LinkedList<T> _nextChunk = new LinkedList<>();
    private final @NotNull Object _nextChunkLock = new Object();
    private @NotNull Instant _nextSizeWarning = Instant.now();
    private final @NotNull Duration _sizeWarningInterval = Duration.ofSeconds(10);
    private boolean _warningTriggered = false;
}
