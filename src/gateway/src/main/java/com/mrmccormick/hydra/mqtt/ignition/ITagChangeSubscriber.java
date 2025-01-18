package com.mrmccormick.hydra.mqtt.ignition;

import com.inductiveautomation.ignition.common.tags.model.TagPath;
import org.jetbrains.annotations.NotNull;

public interface ITagChangeSubscriber {
    void onTagPathChange(@NotNull TagPath oldPath, @NotNull TagPath newPath);

    void onTagValueChange(@NotNull TagChangeEvent tagChangeEvent);
}
