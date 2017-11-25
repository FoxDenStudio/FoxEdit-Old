/*
 * This file is part of FoxEdit, licensed under the MIT License (MIT).
 *
 * Copyright (c) gravityfox - https://gravityfox.net/
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.foxdenstudio.sponge.foxedit.plugin;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import net.foxdenstudio.sponge.foxcore.plugin.util.FCPUtil;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.*;
import org.spongepowered.api.event.world.LoadWorldEvent;
import org.spongepowered.api.event.world.UnloadWorldEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.SpongeExecutorService;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ArchetypeVolume;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Plugin(id = "foxedit",
        name = "FoxEdit",
        dependencies = {
                @Dependency(id = "foxcore")
        },
        description = "A world editing plugin built for SpongeAPI. Requires FoxCore.",
        authors = {"gravityfox", "d4rkfly3r"},
        url = "https://github.com/FoxDenStudio/FoxEdit")
public final class FoxEditMain {

    /**
     * FoxEditMain instance object.
     */
    private static FoxEditMain instanceField;
    public Cause pluginCause;

    @Inject
    private Logger logger;

    @Inject
    private Game game;

    @Inject
    private EventManager eventManager;

    @Inject
    @ConfigDir(sharedRoot = true)
    private Path configDirectory;

    @Inject
    private PluginContainer container;

    private UserStorageService userStorage;
    private EconomyService economyService = null;

    private boolean loaded = false;
    private AtomicBoolean doneProcessing = new AtomicBoolean(false);


    /**
     * @return The current instance of the FoxEditMain object.
     */
    public static FoxEditMain instance() {
        return instanceField;
    }

    //my uuid - f275f223-1643-4fac-9fb8-44aaf5b4b371

    public static Cause getCause() {
        return instance().pluginCause;
    }

    @Listener
    public void construct(GameConstructionEvent event) {
        this.pluginCause = Cause.builder().named("plugin", this.container).build();
        instanceField = this;
    }

    @Listener
    public void preInit(GamePreInitializationEvent event) {
        this.logger.info("Beginning FoxEdit initialization");
        this.logger.info("Version: " + this.container.getVersion().orElse("Unknown"));
    }

    @Listener
    public void init(GameInitializationEvent event) {
        this.logger.info("Getting User Storage");
        this.userStorage = this.game.getServiceManager().provide(UserStorageService.class).get();
        this.logger.info("Registering event listeners");
        this.registerListeners();
    }

    @Listener
    public void registerCommands(GameInitializationEvent event) {
        this.logger.info("Registering commands");
        Sponge.getCommandManager().register(this, new CommandCallable() {
            @Override
            public CommandResult process(CommandSource source, final String arguments) {
                if (arguments.isEmpty()) {
//                    MutableBlockVolume blockVolume = ((Player) source).getWorld().getBlockView(Vector3i.from(50, 50, 50), Vector3i.from(200, 200, 200)).getBlockCopy();
//                    System.out.println("Visiting Volume: " + blockVolume.getClass().getName());
//                    blockVolume.getBlockWorker(FoxEditMain.this.pluginCause).iterate((volume, x, y, z) -> volume.setBlockType(x, y, z, BlockTypes.AIR, FoxEditMain.this.pluginCause));
                    final ArchetypeVolume archetypeVolume = Sponge.getRegistry().getExtentBufferFactory().createArchetypeVolume(Vector3i.from(25, 25, 25), Vector3i.from(0, 0, 0));
                    CompletableFuture completableFuture = CompletableFuture.runAsync(() -> {
                        System.out.println("Generating... ");
                        source.sendMessage(Text.of("Generating Region"));
                        archetypeVolume.getBlockWorker(FoxEditMain.this.pluginCause).fill((x, y, z) -> BlockState.builder().blockType(BlockTypes.TORCH).build(), FoxEditMain.this.pluginCause);
                        FoxEditMain.this.doneProcessing.set(true);
                    });
                    final SpongeExecutorService syncExecutor = Sponge.getScheduler().createSyncExecutor(FoxEditMain.this);
                    final Task task = syncExecutor.scheduleAtFixedRate(() -> {
                        if (FoxEditMain.this.doneProcessing.get()) {
                            System.out.println("Merging....");
                            source.sendMessage(Text.of("Merging Region"));
                            System.out.println(System.currentTimeMillis());
                            archetypeVolume.apply(((Player) source).getLocation(), BlockChangeFlag.NONE, FoxEditMain.this.pluginCause);
                            System.out.println(System.currentTimeMillis());
                            System.out.println("Generating Complete... ");
                            source.sendMessage(Text.of("Generating Region"));
                            FoxEditMain.this.doneProcessing.set(false);
                        }
                    }, 5, 5, TimeUnit.SECONDS).getTask();
                    syncExecutor.schedule((Runnable) task::cancel, 60, TimeUnit.SECONDS);
//                    final Thread callingThread = Thread.currentThread();
//                    new Thread(() -> {
//                        synchronized (callingThread) {
//                        }
//                    }).start();
//                    final MutableBlockVolume threadSafeBlockBuffer = Sponge.getRegistry().getExtentBufferFactory().createThreadSafeBlockBuffer(150, 150, 150);
//                    threadSafeBlockBuffer.getBlockWorker(FoxEditMain.this.pluginCause).iterate((volume, x, y, z) -> volume.setBlockType(x, y, z, BlockTypes.AIR, FoxEditMain.this.pluginCause) );
                } else {
                    FCPUtil.getPositions(source).forEach(position -> {
                        final Optional<BlockType> blockType = Sponge.getRegistry().getType(BlockType.class, arguments);
                        blockType.ifPresent((bT) ->
                                ((Player) source).getWorld()
                                        .getLocation(position)
                                        .setBlock(BlockState.builder().blockType(bT).build(), FoxEditMain.this.pluginCause));
                    });
                }
                return CommandResult.empty();
            }

            @Override
            public List<String> getSuggestions(CommandSource source, String arguments, @Nullable Location<World> targetPosition) {
                return Lists.newArrayList();
            }

            @Override
            public boolean testPermission(CommandSource source) {
                return true;
            }

            @Override
            public Optional<Text> getShortDescription(CommandSource source) {
                return Optional.empty();
            }

            @Override
            public Optional<Text> getHelp(CommandSource source) {
                return Optional.empty();
            }

            @Override
            public Text getUsage(CommandSource source) {
                return null;
            }
        }, "fefill");
    }

    @Listener
    public void configurePermissions(GamePostInitializationEvent event) {
        this.logger.info("Configuring permissions");
        PermissionService service = this.game.getServiceManager().provide(PermissionService.class).get();
        service.getDefaults().getTransientSubjectData().setPermission(SubjectData.GLOBAL_CONTEXT, "foxedit.override", Tristate.FALSE);
    }

    @Listener
    public void serverStarting(GameStartingServerEvent event) {
        this.loaded = true;
        this.logger.info("Finished loading FoxEdit!");
    }

    @Listener
    public void serverStopping(GameStoppingServerEvent event) {
    }

    @Listener
    public void worldUnload(UnloadWorldEvent event) {
        this.logger.info("Unloading world \"" + event.getTargetWorld().getName() + "\"");
    }

    @Listener
    public void worldLoad(LoadWorldEvent event) {
    }

    /**
     * A private method that registers the Listener class and the corresponding event class.
     */
    private void registerListeners() {
    }

    /**
     * @return A Logger instance for this plugin.
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Method that when called will return a UserStorageService object that can be used to store or retrieve data corresponding to a specific player.
     *
     * @return UserStorageService object.
     */
    public UserStorageService getUserStorage() {
        if (this.userStorage == null) {
            this.logger.info("Getting User Storage");
            this.userStorage = this.game.getServiceManager().provide(UserStorageService.class).get();
        }
        return this.userStorage;
    }

    /**
     * @return A File object corresponding to the config of the plugin.
     */
    public Path getConfigDirectory() {
        return this.configDirectory;
    }

    /**
     * Will return true or false depending on if the plugin has loaded properly or not.
     *
     * @return Depending on the loaded variable
     */
    public boolean isLoaded() {
        return this.loaded;
    }
}
