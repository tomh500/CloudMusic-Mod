package fengliu.cloudmusic.command;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.music163.*;
import fengliu.cloudmusic.music163.data.*;
import fengliu.cloudmusic.util.MusicPlayer;
import fengliu.cloudmusic.util.TextClickItem;
import fengliu.cloudmusic.util.page.Page;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class MusicCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("cloudmusic");
    private static final LoginMusic163 loginMusic163 = new LoginMusic163();
    private static Music163 music163 = new Music163(Configs.LOGIN.COOKIE.getStringValue());
    private static MusicPlayer player = new MusicPlayer(new ArrayList<>());
    private static Page page = null;
    private static Object data = null;
    private static My my = null;
    public static boolean loadQRCode = false;
    private static final Component[] helps = {
            Component.translatable("cloudmusic.help.music"),
            Component.translatable("cloudmusic.help.music.play"),
            Component.translatable("cloudmusic.help.music.like"),
            Component.translatable("cloudmusic.help.music.unlike"),
            Component.translatable("cloudmusic.help.music.similar.music"),
            Component.translatable("cloudmusic.help.music.similar.playlist"),
            Component.translatable("cloudmusic.help.music.comment"),
            Component.translatable("cloudmusic.help.music.hot.comment"),
            Component.translatable("cloudmusic.help.music.send.comment"),

            Component.translatable("cloudmusic.help.playlist"),
            Component.translatable("cloudmusic.help.playlist.play"),
            Component.translatable("cloudmusic.help.playlist.subscribe"),
            Component.translatable("cloudmusic.help.playlist.unsubscribe"),
            Component.translatable("cloudmusic.help.playlist.add"),
            Component.translatable("cloudmusic.help.playlist.del"),
            Component.translatable("cloudmusic.help.playlist.comment"),
            Component.translatable("cloudmusic.help.playlist.hot.comment"),
            Component.translatable("cloudmusic.help.playlist.send.comment"),

            Component.translatable("cloudmusic.help.artist"),
            Component.translatable("cloudmusic.help.artist.top"),
            Component.translatable("cloudmusic.help.artist.album"),
            Component.translatable("cloudmusic.help.artist.similar"),
            Component.translatable("cloudmusic.help.artist.subscribe"),
            Component.translatable("cloudmusic.help.artist.unsubscribe"),

            Component.translatable("cloudmusic.help.album"),
            Component.translatable("cloudmusic.help.album.play"),
            Component.translatable("cloudmusic.help.album.subscribe"),
            Component.translatable("cloudmusic.help.album.unsubscribe"),
            Component.translatable("cloudmusic.help.album.comment"),
            Component.translatable("cloudmusic.help.album.hot.comment"),
            Component.translatable("cloudmusic.help.album.send.comment"),

            Component.translatable("cloudmusic.help.dj"),
            Component.translatable("cloudmusic.help.dj.play"),
            Component.translatable("cloudmusic.help.dj.music"),
            Component.translatable("cloudmusic.help.dj.music.play"),
            Component.translatable("cloudmusic.help.dj.music.send.comment"),
            Component.translatable("cloudmusic.help.dj.music.comment"),
            Component.translatable("cloudmusic.help.dj.music.hot.comment"),
            Component.translatable("cloudmusic.help.dj.subscribe"),
            Component.translatable("cloudmusic.help.dj.unsubscribe"),
            Component.translatable("cloudmusic.help.dj.send.comment"),
            Component.translatable("cloudmusic.help.dj.comment"),
            Component.translatable("cloudmusic.help.dj.hot.comment"),

            Component.translatable("cloudmusic.help.comment"),
            Component.translatable("cloudmusic.help.comment.floors"),
            Component.translatable("cloudmusic.help.comment.like"),
            Component.translatable("cloudmusic.help.comment.unlike"),
            Component.translatable("cloudmusic.help.comment.delete"),
            Component.translatable("cloudmusic.help.comment.reply"),

            Component.translatable("cloudmusic.help.user"),
            Component.translatable("cloudmusic.help.user.playlist"),
            Component.translatable("cloudmusic.help.user.dj"),
            Component.translatable("cloudmusic.help.user.like"),
            Component.translatable("cloudmusic.help.user.record.all"),
            Component.translatable("cloudmusic.help.user.record.week"),

            Component.translatable("cloudmusic.help.my"),
            Component.translatable("cloudmusic.help.my.fm"),
            Component.translatable("cloudmusic.help.my.intelligence"),
            Component.translatable("cloudmusic.help.my.like"),
            Component.translatable("cloudmusic.help.my.playlist"),
            Component.translatable("cloudmusic.help.my.dj"),
            Component.translatable("cloudmusic.help.my.style"),
            Component.translatable("cloudmusic.help.my.playlist.add"),
            Component.translatable("cloudmusic.help.my.playlist.del"),
            Component.translatable("cloudmusic.help.my.recommend.music"),
            Component.translatable("cloudmusic.help.my.recommend.playlist"),
            Component.translatable("cloudmusic.help.my.recommend.history"),
            Component.translatable("cloudmusic.help.my.recommend.history.date"),
            Component.translatable("cloudmusic.help.my.sublist.album"),
            Component.translatable("cloudmusic.help.my.sublist.artist"),
            Component.translatable("cloudmusic.help.my.sublist.dj"),
            Component.translatable("cloudmusic.help.my.record.music"),
            Component.translatable("cloudmusic.help.my.record.djmusic"),
            Component.translatable("cloudmusic.help.my.record.playlist"),
            Component.translatable("cloudmusic.help.my.record.album"),
            Component.translatable("cloudmusic.help.my.record.dj"),

            Component.translatable("cloudmusic.help.style"),
            Component.translatable("cloudmusic.help.style.all"),
            Component.translatable("cloudmusic.help.style.children"),
            Component.translatable("cloudmusic.help.style.music"),
            Component.translatable("cloudmusic.help.style.playlist"),
            Component.translatable("cloudmusic.help.style.artist"),
            Component.translatable("cloudmusic.help.style.album"),

            Component.translatable("cloudmusic.help.top.list"),
            Component.translatable("cloudmusic.help.top.artist"),
            Component.translatable("cloudmusic.help.top.playlist.highquality.tags"),
            Component.translatable("cloudmusic.help.top.playlist.highquality"),
            Component.translatable("cloudmusic.help.top.playlist.tags"),
            Component.translatable("cloudmusic.help.top.playlist.tags.hot"),
            Component.translatable("cloudmusic.help.top.playlist"),

            Component.translatable("cloudmusic.help.search.music"),
            Component.translatable("cloudmusic.help.search.album"),
            Component.translatable("cloudmusic.help.search.artist"),
            Component.translatable("cloudmusic.help.search.playlist"),
            Component.translatable("cloudmusic.help.search.dj"),

            Component.translatable("cloudmusic.help.login.email"),
            Component.translatable("cloudmusic.help.login.captcha"),
            Component.translatable("cloudmusic.help.login.captcha.login"),
            Component.translatable("cloudmusic.help.login.captcha.phone"),
            Component.translatable("cloudmusic.help.login.qr"),

            Component.translatable("cloudmusic.help.volume"),
            Component.translatable("cloudmusic.help.volume.volume"),
            Component.translatable("cloudmusic.help.lyric"),
            Component.translatable("cloudmusic.help.musicinfo"),

            Component.translatable("cloudmusic.help.page.prev"),
            Component.translatable("cloudmusic.help.page.next"),
            Component.translatable("cloudmusic.help.page.to"),

            Component.translatable("cloudmusic.help.playing"),
            Component.translatable("cloudmusic.help.playing.all"),

            Component.translatable("cloudmusic.help.stop"),
            Component.translatable("cloudmusic.help.continue"),
            Component.translatable("cloudmusic.help.prev"),
            Component.translatable("cloudmusic.help.next"),
            Component.translatable("cloudmusic.help.to"),
            Component.translatable("cloudmusic.help.del"),
            Component.translatable("cloudmusic.help.trash"),
            Component.translatable("cloudmusic.help.random"),
            Component.translatable("cloudmusic.help.exit"),
            Component.translatable("cloudmusic.help.cloudmusic"),
    };
    private static final List<Component> helpsList = new ArrayList<>();

    public static MusicPlayer getPlayer() {
        return player;
    }

    public static void setPage(Page Page) {
        page = Page;
    }

    public static My getMy(boolean reset) {
        if (my == null || reset) {
            my = music163.my();
        }

        return my;
    }

    /**
     * 重置歌曲播放器
     *
     * @param musics 歌曲列表
     */
    private static void resetPlayer(List<IMusic> musics) {
        try {
            player.exit();
        } catch (Exception e) {

        }
        player = new MusicPlayer(musics);
    }

    /**
     * 重置歌曲播放器
     *
     * @param newPlayer 播放器
     */
    private static void resetPlayer(MusicPlayer newPlayer) {
        try {
            player.exit();
        } catch (Exception e) {

        }
        player = newPlayer;
    }

    /**
     * 重置歌曲播放器
     *
     * @param music 歌曲
     */
    private static void resetPlayer(IMusic music) {
        List<IMusic> musics = new ArrayList<>();
        musics.add(music);

        resetPlayer(musics);
    }

    private static void resetCookie(String cookie) {
        if (cookie == null || music163.getHttpClient().getCookies().equals(cookie)) {
            return;
        }

        Configs.LOGIN.COOKIE.setValueFromString(cookie);
        music163 = new Music163(cookie);
        getMy(true);

        Configs.INSTANCE.save();
    }

    private interface Job {
        void fun(CommandContext<FabricClientCommandSource> context) throws Exception;
    }

    /**
     * 新开线程运行指令
     *
     * @param context 指令上下文
     * @param job     任务
     */
    private static void runCommand(CommandContext<FabricClientCommandSource> context, Job job) {
        Thread commandThread = new Thread(() -> {
            LOGGER.info("[CloudMusic][Cmd] 异步指令开始");
            try {
                job.fun(context);
                LOGGER.info("[CloudMusic][Cmd] 异步指令完成");
            } catch (Exception err) {
                LOGGER.info("[CloudMusic][Cmd] 异步指令异常", err);
                context.getSource().sendFeedback(Component.literal(err.getMessage()));
            }
        });
        commandThread.setDaemon(true);
        commandThread.setName("CloudMusic Thread");
        commandThread.start();
    }

    public static void registerAll() {
        LiteralArgumentBuilder<FabricClientCommandSource> CloudMusic = literal("cloudmusic");
        LiteralArgumentBuilder<FabricClientCommandSource> Music = literal("music");
        LiteralArgumentBuilder<FabricClientCommandSource> PlayList = literal("playlist");
        LiteralArgumentBuilder<FabricClientCommandSource> Artist = literal("artist");
        LiteralArgumentBuilder<FabricClientCommandSource> Album = literal("album");
        LiteralArgumentBuilder<FabricClientCommandSource> Dj = literal("dj");
        LiteralArgumentBuilder<FabricClientCommandSource> User = literal("user");
        LiteralArgumentBuilder<FabricClientCommandSource> My = literal("my");
        LiteralArgumentBuilder<FabricClientCommandSource> Style = literal("style");
        LiteralArgumentBuilder<FabricClientCommandSource> Top = literal("top");
        LiteralArgumentBuilder<FabricClientCommandSource> Playing = literal("playing");
        LiteralArgumentBuilder<FabricClientCommandSource> Search = literal("search");
        LiteralArgumentBuilder<FabricClientCommandSource> Volume = literal("volume");
        LiteralArgumentBuilder<FabricClientCommandSource> Page = literal("page");
        LiteralArgumentBuilder<FabricClientCommandSource> Login = literal("login");

        Collections.addAll(helpsList, helps);
        CloudMusic.executes(context -> {
            page = new Page(helpsList) {
                @Override
                protected TextClickItem putPageItem(Object data) {
                    return new TextClickItem((MutableComponent) data, "");
                }
            };
            page.setInfoText(Component.translatable("cloudmusic.info.page.help"));
            page.look(context.getSource());
            return Command.SINGLE_SUCCESS;
        });

        // cloudmusic music id
        CloudMusic.then(Music.then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.music(LongArgumentType.getLong(context, "id"));
                        ((Music) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic music play id
        CloudMusic.then(Music.then(literal("play").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Music music = music163.music(LongArgumentType.getLong(context, "id"));
                        resetPlayer(music);
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.music.play", music.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic music like id
        CloudMusic.then(Music.then(literal("like").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Music music = music163.music(LongArgumentType.getLong(context, "id"));
                        music.like();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.music.like", music.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic music unlike id
        CloudMusic.then(Music.then(literal("unlike").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Music music = music163.music(LongArgumentType.getLong(context, "id"));
                        music.unlike();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.music.unlike", music.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        LiteralArgumentBuilder<FabricClientCommandSource> Similar = literal("similar");

        // cloudmusic music similar music
        CloudMusic.then(Music.then(Similar.then(literal("music").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Music music = music163.music(LongArgumentType.getLong(context, "id"));
                        page = music.similar();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.music.similar", music.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic music similar playlist
        CloudMusic.then(Music.then(Similar.then(literal("playlist").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Music music = music163.music(LongArgumentType.getLong(context, "id"));
                        page = music.similarPlaylist();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.music.similar.playlist", music.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));


        // cloudmusic music comment id
        CloudMusic.then(Music.then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Music music = music163.music(LongArgumentType.getLong(contextData, "id"));
                        page = music.comments(false);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.music.comments", music.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic music hotComment id
        CloudMusic.then(Music.then(literal("hotComment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Music music = music163.music(LongArgumentType.getLong(contextData, "id"));
                        page = music.comments(true);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.music.hot.comments", music.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic music send comment id content
        CloudMusic.then(Music.then(literal("send").then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("content", StringArgumentType.string()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                Music music = music163.music(LongArgumentType.getLong(context, "id"));
                                music.send(StringArgumentType.getString(context, "content"));
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.send.comment", music.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                )))));

        // cloudmusic playlist id
        CloudMusic.then(PlayList.then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.playlist(LongArgumentType.getLong(context, "id"));
                        ((PlayList) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic playlist play id
        CloudMusic.then(PlayList.then(literal("play").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        PlayList playList = music163.playlist(LongArgumentType.getLong(context, "id"));
                        resetPlayer(playList.getMusics());
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.playlist.play", playList.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic playlist send comment id content
        CloudMusic.then(PlayList.then(literal("send").then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("content", StringArgumentType.string()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                PlayList playlist = music163.playlist(LongArgumentType.getLong(context, "id"));
                                playlist.send(StringArgumentType.getString(context, "content"));
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.send.comment", playlist.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                )))));


        // cloudmusic playlist comment id
        CloudMusic.then(PlayList.then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        PlayList playList = music163.playlist(LongArgumentType.getLong(contextData, "id"));
                        page = playList.comments(false);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.playlist.comments", playList.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic playlist hotComment id
        CloudMusic.then(PlayList.then(literal("hotComment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        PlayList playList = music163.playlist(LongArgumentType.getLong(contextData, "id"));
                        page = playList.comments(true);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.playlist.hot.comments", playList.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic playlist subscribe id
        CloudMusic.then(PlayList.then(literal("subscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        PlayList playList = music163.playlist(LongArgumentType.getLong(context, "id"));
                        playList.subscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.playlist.subscribe", playList.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic playlist unsubscribe id
        CloudMusic.then(PlayList.then(literal("unsubscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        PlayList playList = music163.playlist(LongArgumentType.getLong(context, "id"));
                        playList.unsubscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.playlist.unsubscribe", playList.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic playlist add id musicId
        CloudMusic.then(PlayList.then(literal("add").then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("musicId", LongArgumentType.longArg()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                long musicId = LongArgumentType.getLong(context, "musicId");
                                PlayList playList = music163.playlist(LongArgumentType.getLong(context, "id"));
                                playList.add(musicId);
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.playlist.add", playList.name, musicId));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                ))
        ));

        // cloudmusic playlist del id musicId
        CloudMusic.then(PlayList.then(literal("del").then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("musicId", LongArgumentType.longArg()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                long musicId = LongArgumentType.getLong(context, "musicId");
                                PlayList playList = music163.playlist(LongArgumentType.getLong(context, "id"));
                                playList.del(musicId);
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.playlist.del", playList.name, musicId));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                ))
        ));

        // cloudmusic artist id
        CloudMusic.then(Artist.then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.artist(LongArgumentType.getLong(context, "id"));
                        ((Artist) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic artist top id
        CloudMusic.then(Artist.then(literal("top").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Artist artist = music163.artist(LongArgumentType.getLong(context, "id"));
                        resetPlayer(artist.topSong());
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.artist.top.play", artist.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic artist album id
        CloudMusic.then(Artist.then(literal("album").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Artist artist = music163.artist(LongArgumentType.getLong(context, "id"));
                        page = artist.albumPage();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.artist.album", artist.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic artist similar id
        CloudMusic.then(Artist.then(literal("similar").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Artist artist = music163.artist(LongArgumentType.getLong(context, "id"));
                        page = artist.similar();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.artist.similar", artist.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic artist subscribe id
        CloudMusic.then(Artist.then(literal("subscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Artist artist = music163.artist(LongArgumentType.getLong(context, "id"));
                        artist.subscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.artist.subscribe", artist.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic artist unsubscribe id
        CloudMusic.then(Artist.then(literal("unsubscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Artist artist = music163.artist(LongArgumentType.getLong(context, "id"));
                        artist.unsubscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.artist.unsubscribe", artist.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic artist music id
        // CloudMusic.then(Artist.then(literal("music").then(
        //     argument("id", LongArgumentType.longArg()).executes(contextData -> {
        //         runCommand(contextData, context -> {
        //             resetPlayer((new MusicPlayList(music163.artist(LongArgumentType.getLong(context, "id")).music())).createMusicPlayer(false));
        //             player.start();
        //         });
        //         return Command.SINGLE_SUCCESS;
        //     })
        // )));

        // cloudmusic album id
        CloudMusic.then(Album.then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.album(LongArgumentType.getLong(context, "id"));
                        ((Album) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic album play id
        CloudMusic.then(Album.then(literal("play").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Album album = music163.album(LongArgumentType.getLong(context, "id"));
                        resetPlayer(album.getMusics());
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.album.play", album.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic album send comment id content
        CloudMusic.then(Album.then(literal("send").then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("content", StringArgumentType.string()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                Album album = music163.album(LongArgumentType.getLong(context, "id"));
                                album.send(StringArgumentType.getString(context, "content"));
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.send.comment", album.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                )))));


        // cloudmusic album comment id
        CloudMusic.then(Album.then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Album album = music163.album(LongArgumentType.getLong(context, "id"));
                        page = album.comments(false);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.album.comments", album.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic album hotComment id
        CloudMusic.then(Album.then(literal("hotComment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Album album = music163.album(LongArgumentType.getLong(context, "id"));
                        page = album.comments(true);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.album.hot.comments", album.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic album subscribe id
        CloudMusic.then(Album.then(literal("subscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Album album = music163.album(LongArgumentType.getLong(context, "id"));
                        album.subscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.album.subscribe", album.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic album unsubscribe id
        CloudMusic.then(Album.then(literal("unsubscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        Album album = music163.album(LongArgumentType.getLong(context, "id"));
                        album.unsubscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.album.unsubscribe", album.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic dj id
        CloudMusic.then(Dj.then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.djRadio(LongArgumentType.getLong(context, "id"));
                        ((DjRadio) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic dj play id
        CloudMusic.then(Dj.then(literal("play").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjRadio djRadio = music163.djRadio(LongArgumentType.getLong(context, "id"));
                        resetPlayer(djRadio);
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.dj.play", djRadio.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic dj send comment id content
        CloudMusic.then(Dj.then(literal("send").then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("content", StringArgumentType.string()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                DjRadio djRadio = music163.djRadio(LongArgumentType.getLong(context, "id"));
                                djRadio.send(StringArgumentType.getString(context, "content"));
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.send.comment", djRadio.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                )))));

        // cloudmusic dj comment id
        CloudMusic.then(Dj.then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjRadio djRadio = music163.djRadio(LongArgumentType.getLong(context, "id"));
                        page = djRadio.comments(false);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.dj.radio.comments", djRadio.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic dj hotComment id
        CloudMusic.then(Dj.then(literal("hotComment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjRadio djRadio = music163.djRadio(LongArgumentType.getLong(context, "id"));
                        page = djRadio.comments(true);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.dj.radio.hot.comments", djRadio.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));


        LiteralArgumentBuilder<FabricClientCommandSource> DjMusic = literal("music");

        // cloudmusic dj music id
        CloudMusic.then(Dj.then(DjMusic.then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.djMusic(LongArgumentType.getLong(context, "id"));
                        ((DjMusic) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic dj music play id
        CloudMusic.then(Dj.then(DjMusic.then(literal("play").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjMusic music = music163.djMusic(LongArgumentType.getLong(context, "id"));
                        resetPlayer(music);
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.dj.music.play", music.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic dj music send comment id content
        CloudMusic.then(Dj.then(DjMusic.then(literal("send").then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("content", StringArgumentType.string()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                DjMusic music = music163.djMusic(LongArgumentType.getLong(context, "id"));
                                music.send(StringArgumentType.getString(context, "content"));

                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.send.comment", music.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                ))))));

        // cloudmusic dj music comment id
        CloudMusic.then(Dj.then(DjMusic.then(literal("comment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjMusic music = music163.djMusic(LongArgumentType.getLong(context, "id"));
                        page = music.comments(false);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.dj.music.comments", music.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic dj music hotComment id
        CloudMusic.then(Dj.then(DjMusic.then(literal("hotComment").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjMusic music = music163.djMusic(LongArgumentType.getLong(context, "id"));
                        page = music.comments(true);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.dj.music.hot.comments", music.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic dj subscribe id
        CloudMusic.then(Dj.then(literal("subscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjRadio djRadio = music163.djRadio(LongArgumentType.getLong(context, "id"));
                        djRadio.subscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.dj.subscribe", djRadio.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic dj unsubscribe id
        CloudMusic.then(Dj.then(literal("unsubscribe").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        DjRadio djRadio = music163.djRadio(LongArgumentType.getLong(context, "id"));
                        djRadio.unsubscribe();
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.dj.unsubscribe", djRadio.name));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic user id
        CloudMusic.then(User.then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.user(LongArgumentType.getLong(context, "id"));
                        ((User) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic user playlist id
        CloudMusic.then(User.then(literal("playlist").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        User user = music163.user(LongArgumentType.getLong(context, "id"));
                        page = user.playListsPage();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.user.playlist", user.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                }))
        ));

        // cloudmusic user dj id
        CloudMusic.then(User.then(literal("dj").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        User user = music163.user(LongArgumentType.getLong(context, "id"));
                        page = user.djRadio();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.user.dj", user.name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                }))
        ));

        // cloudmusic user like id
        CloudMusic.then(User.then(literal("like").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        User user = music163.user(LongArgumentType.getLong(context, "id"));
                        resetPlayer(user.likeMusicPlayList().getMusics());
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.like", user.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                }))
        ));

        LiteralArgumentBuilder<FabricClientCommandSource> Record = literal("record");

        // cloudmusic user record all id
        CloudMusic.then(User.then(Record.then(literal("all").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        User user = music163.user(LongArgumentType.getLong(context, "id"));
                        resetPlayer(user.recordAll());
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.user.record.all", user.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })))
        ));

        // cloudmusic user record week id
        CloudMusic.then(User.then(Record.then(literal("week").then(
                argument("id", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        User user = music163.user(LongArgumentType.getLong(context, "id"));
                        resetPlayer(user.recordWeek());
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.user.record.week", user.name));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
                })))
        ));

        // cloudmusic my
        CloudMusic.then(My.executes(contextData -> {
            runCommand(contextData, context -> {
                getMy(true).printToChatHud(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }));

        // cloudmusic my like
        CloudMusic.then(My.then(literal("like").executes(contextData -> {
            runCommand(contextData, context -> {
                resetPlayer(getMy(false).likeMusicPlayList().getMusics());
                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.like", getMy(false).name));
                player.start();
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic my fm
        CloudMusic.then(My.then(literal("fm").executes(contextData -> {
            runCommand(contextData, context -> {
                resetPlayer(new Fm(getMy(false)));
                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.fm"));
                player.start();
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic my intelligence
        CloudMusic.then(My.then(literal("intelligence").executes(contextData -> {
            runCommand(contextData, context -> {
                resetPlayer(getMy(false).intelligencePlayMode());
                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.intelligence"));
                player.start();
            });
            return Command.SINGLE_SUCCESS;
        })));

        LiteralArgumentBuilder<FabricClientCommandSource> MyPlayList = literal("playlist");

        // cloudmusic my playlist
        CloudMusic.then(My.then(MyPlayList.executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).playListsPage();
                page.setInfoText(Component.translatable("cloudmusic.info.page.user.playlist", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic my dj
        CloudMusic.then(My.then(literal("dj").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).djRadio();
                page.setInfoText(Component.translatable("cloudmusic.info.page.user.dj", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic my style
        CloudMusic.then(My.then(literal("style").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).preferenceStyles();
                page.setInfoText(Component.translatable("cloudmusic.info.page.preference.style", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        LiteralArgumentBuilder<FabricClientCommandSource> PlayRecord = literal("record");

        // cloudmusic my record music
        CloudMusic.then(My.then(PlayRecord.then(literal("music").executes(contextData -> {
            runCommand(contextData, context -> {
                resetPlayer(getMy(false).recordPlayMusic());
                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.record.music", getMy(false).name));
                player.start();
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my record djmusic
        CloudMusic.then(My.then(PlayRecord.then(literal("djmusic").executes(contextData -> {
            runCommand(contextData, context -> {
                resetPlayer(getMy(false).recordPlayDjMusic());
                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.record.djmusic", getMy(false).name));
                player.start();
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my record playlist
        CloudMusic.then(My.then(PlayRecord.then(literal("playlist").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).recordPlayPlayList();
                page.setInfoText(Component.translatable("cloudmusic.info.page.record.playlist", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my record album
        CloudMusic.then(My.then(PlayRecord.then(literal("album").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).recordPlayAlbum();
                page.setInfoText(Component.translatable("cloudmusic.info.page.record.album", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my record dj
        CloudMusic.then(My.then(PlayRecord.then(literal("dj").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).recordPlayDj();
                page.setInfoText(Component.translatable("cloudmusic.info.page.record.dj", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my playlist add musicId
        CloudMusic.then(My.then(MyPlayList.then(literal("add").then(
                argument("musicId", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        page = getMy(false).playListSetMusic(LongArgumentType.getLong(context, "musicId"), "add");
                        page.setInfoText(Component.translatable("cloudmusic.info.page.user.playlist.add", getMy(false).name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic my playlist del musicId
        CloudMusic.then(My.then(MyPlayList.then(literal("del").then(
                argument("musicId", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        page = getMy(false).playListSetMusic(LongArgumentType.getLong(context, "musicId"), "del");
                        page.setInfoText(Component.translatable("cloudmusic.info.page.user.playlist.del", getMy(false).name));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        LiteralArgumentBuilder<FabricClientCommandSource> Recommend = literal("recommend");

        // cloudmusic my recommend music
        CloudMusic.then(My.then(Recommend.then(literal("music").executes(contextData -> {
            runCommand(contextData, context -> {
                resetPlayer(getMy(false).recommendSongs());
                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.recommend.music"));
                player.start();
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my recommend playlist
        CloudMusic.then(My.then(Recommend.then(literal("playlist").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).recommendResource();
                page.setInfoText(Component.translatable("cloudmusic.info.page.recommend.playlist", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my recommend history
        CloudMusic.then(My.then(Recommend.then(literal("history").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).recommendHistorySongsRecent();
                page.setInfoText(Component.translatable("cloudmusic.info.page.recommend.history", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my recommend history date
        CloudMusic.then(My.then(Recommend.then(literal("history").then(
                argument("date", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String date = StringArgumentType.getString(context, "date");
                        resetPlayer(getMy(false).recommendHistorySongs(date));
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.recommend.history.music", date));
                        player.start();
                    });
                    return Command.SINGLE_SUCCESS;
        })))));

        LiteralArgumentBuilder<FabricClientCommandSource> Sublist = literal("sublist");

        // cloudmusic my sublist album
        CloudMusic.then(My.then(Sublist.then(literal("album").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).sublistAlbum();
                page.setInfoText(Component.translatable("cloudmusic.info.page.sublist.album", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my sublist artist
        CloudMusic.then(My.then(Sublist.then(literal("artist").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).sublistArtist();
                page.setInfoText(Component.translatable("cloudmusic.info.page.sublist.artist", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic my sublist dj
        CloudMusic.then(My.then(Sublist.then(literal("dj").executes(contextData -> {
            runCommand(contextData, context -> {
                page = getMy(false).sublistDjRadio();
                page.setInfoText(Component.translatable("cloudmusic.info.page.sublist.dj", getMy(false).name));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic style id
        CloudMusic.then(Style.then(
                argument("id", IntegerArgumentType.integer()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        data = music163.style(IntegerArgumentType.getInteger(context, "id"));
                        ((StyleTag) data).printToChatHud(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic style all
        CloudMusic.then(Style.then(literal("all").executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.styleList();
                page.setInfoText(Component.translatable("cloudmusic.info.page.style"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic style children id
        CloudMusic.then(Style.then(literal("children").then(
                argument("id", IntegerArgumentType.integer()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        StyleTag style = music163.style(IntegerArgumentType.getInteger(context, "id"));
                        page = style.childrenStyles();
                        if (page == null) {
                            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.style.not.children", style.name, style.enName));
                            return;
                        }
                        page.setInfoText(Component.translatable("cloudmusic.info.page.style.children", style.name, style.enName));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic style music id
        CloudMusic.then(Style.then(literal("music").then(
                argument("id", IntegerArgumentType.integer()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        StyleTag style = music163.style(IntegerArgumentType.getInteger(context, "id"));
                        page = style.music();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.style.music", style.name, style.enName));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic style playlist id
        CloudMusic.then(Style.then(literal("playlist").then(
                argument("id", IntegerArgumentType.integer()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        StyleTag style = music163.style(IntegerArgumentType.getInteger(context, "id"));
                        page = style.playlist();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.style.playlist", style.name, style.enName));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic style artist id
        CloudMusic.then(Style.then(literal("artist").then(
                argument("id", IntegerArgumentType.integer()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        StyleTag style = music163.style(IntegerArgumentType.getInteger(context, "id"));
                        page = style.artist();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.style.artist", style.name, style.enName));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic style album id
        CloudMusic.then(Style.then(literal("album").then(
                argument("id", IntegerArgumentType.integer()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        StyleTag style = music163.style(IntegerArgumentType.getInteger(context, "id"));
                        page = style.album();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.style.album", style.name, style.enName));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic top list
        CloudMusic.then(Top.then(literal("list").executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.topList();
                page.setInfoText(Component.translatable("cloudmusic.info.page.top.list"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic top artist
        CloudMusic.then(Top.then(literal("artist").executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.topArtistList();
                page.setInfoText(Component.translatable("cloudmusic.info.page.top.artist"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        LiteralArgumentBuilder<FabricClientCommandSource> TopPlayList = literal("playlist");
        LiteralArgumentBuilder<FabricClientCommandSource> HighQuality = literal("highquality");

        // cloudmusic top playlist highquality tags
        CloudMusic.then(Top.then(TopPlayList.then(HighQuality.then(literal("tags").executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.playListHighQualityTags();
                page.setInfoText(Component.translatable("cloudmusic.info.page.top.playlist.highquality.tags"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })))));

        // cloudmusic top playlist highquality
        CloudMusic.then(Top.then(TopPlayList.then(HighQuality.executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.topPlayListHighQuality("全部");
                page.setInfoText(Component.translatable("cloudmusic.info.page.top.playlist.highquality", "全部"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic top playlist highquality tag
        CloudMusic.then(Top.then(TopPlayList.then(HighQuality.then(
                argument("tag", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String tag = StringArgumentType.getString(context, "tag");
                        page = music163.topPlayListHighQuality(tag);
                        if (page == null) {
                            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.tag.not.top.playlist.highquality", tag));
                            return;
                        }
                        page.setInfoText(Component.translatable("cloudmusic.info.page.top.playlist.highquality", tag));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        LiteralArgumentBuilder<FabricClientCommandSource> Tags = literal("tags");

        // cloudmusic top playlist tags
        CloudMusic.then(Top.then(TopPlayList.then(Tags.executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.playListTags();
                page.setInfoText(Component.translatable("cloudmusic.info.page.top.playlist.tags"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        }))));

        // cloudmusic top playlist tags hot
        CloudMusic.then(Top.then(TopPlayList.then(Tags.then(literal("hot").executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.playListTagsHot();
                page.setInfoText(Component.translatable("cloudmusic.info.page.top.playlist.hot.tags"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })))));

        // cloudmusic top playlist
        CloudMusic.then(Top.then(TopPlayList.executes(contextData -> {
            runCommand(contextData, context -> {
                page = music163.topPlayList("全部");
                page.setInfoText(Component.translatable("cloudmusic.info.page.top.playlist", "全部"));
                page.look(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic top playlist tag
        CloudMusic.then(Top.then(TopPlayList.then(
                argument("tag", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String tag = StringArgumentType.getString(context, "tag");
                        page = music163.topPlayList(tag);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.top.playlist", tag));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic search music
        CloudMusic.then(Search.then(literal("music").then(
                argument("key", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String key = StringArgumentType.getString(context, "key");
                        page = music163.searchMusic(key);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.search", key));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic search album
        CloudMusic.then(Search.then(literal("album").then(
                argument("key", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String key = StringArgumentType.getString(context, "key");
                        page = music163.searchAlbum(key);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.search", key));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic search artist
        CloudMusic.then(Search.then(literal("artist").then(
                argument("key", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String key = StringArgumentType.getString(context, "key");
                        page = music163.searchArtist(key);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.search", key));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic search playlist
        CloudMusic.then(Search.then(literal("playlist").then(
                argument("key", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String key = StringArgumentType.getString(context, "key");
                        page = music163.searchPlayList(key);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.search", key));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic search dj
        CloudMusic.then(Search.then(literal("dj").then(
                argument("key", StringArgumentType.string()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        String key = StringArgumentType.getString(context, "key");
                        page = music163.searchDjRadio(key);
                        page.setInfoText(Component.translatable("cloudmusic.info.page.search", key));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));


        LiteralArgumentBuilder<FabricClientCommandSource> Comment = literal("comment");

        // cloudmusic comment id threadId
        CloudMusic.then(Comment.then(
                argument("id", LongArgumentType.longArg()).then(
                        argument("threadId", StringArgumentType.string()).executes(context -> {
                            long id = LongArgumentType.getLong(context, "id");
                            JsonObject json = page.getJsonItem(jsonObject -> jsonObject.get("commentId").getAsLong() == id);
                            if (json == null) {
                                return Command.SINGLE_SUCCESS;
                            }
                            Comment comment = new Comment(music163.getHttpClient(), json, StringArgumentType.getString(context, "threadId"));
                            comment.printToChatHud(context.getSource());
                            return Command.SINGLE_SUCCESS;
                        })
                ))
        );

        // cloudmusic comment floors id threadId
        CloudMusic.then(Comment.then(literal("floors").then(argument("id", LongArgumentType.longArg()).then(
                argument("threadId", StringArgumentType.string()).executes(contextData -> {
                    long id = LongArgumentType.getLong(contextData, "id");
                    JsonObject json = page.getJsonItem(jsonObject -> jsonObject.get("commentId").getAsLong() == id);
                    if (json == null) {
                        return Command.SINGLE_SUCCESS;
                    }
                    Comment comment = new Comment(music163.getHttpClient(), json, StringArgumentType.getString(contextData, "threadId"));
                    runCommand(contextData, context -> {
                        page = comment.floors();
                        page.setInfoText(Component.translatable("cloudmusic.info.page.comment.floors", comment.id));
                        page.look(context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic comment like id threadId
        CloudMusic.then(Comment.then(literal("like").then(argument("id", LongArgumentType.longArg()).then(
                argument("threadId", StringArgumentType.string()).executes(contextData -> {
                    long id = LongArgumentType.getLong(contextData, "id");
                    JsonObject json = page.getJsonItem(jsonObject -> jsonObject.get("commentId").getAsLong() == id);
                    if (json == null) {
                        return Command.SINGLE_SUCCESS;
                    }

                    runCommand(contextData, context -> {
                        Comment comment = new Comment(music163.getHttpClient(), json, StringArgumentType.getString(context, "threadId"));
                        comment.like();

                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.comment.like", comment.content));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic comment unlike id threadId
        CloudMusic.then(Comment.then(literal("unlike").then(argument("id", LongArgumentType.longArg()).then(
                argument("threadId", StringArgumentType.string()).executes(contextData -> {
                    long id = LongArgumentType.getLong(contextData, "id");
                    JsonObject json = page.getJsonItem(jsonObject -> jsonObject.get("commentId").getAsLong() == id);
                    if (json == null) {
                        return Command.SINGLE_SUCCESS;
                    }

                    runCommand(contextData, context -> {
                        Comment comment = new Comment(music163.getHttpClient(), json, StringArgumentType.getString(context, "threadId"));
                        comment.unlike();

                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.comment.unlike", comment.content));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic comment delete id threadId
        CloudMusic.then(Comment.then(literal("delete").then(argument("id", LongArgumentType.longArg()).then(
                argument("threadId", StringArgumentType.string()).executes(contextData -> {
                    long id = LongArgumentType.getLong(contextData, "id");
                    JsonObject json = page.getJsonItem(jsonObject -> jsonObject.get("commentId").getAsLong() == id);
                    if (json == null) {
                        return Command.SINGLE_SUCCESS;
                    }

                    runCommand(contextData, context -> {
                        Comment comment = new Comment(music163.getHttpClient(), json, StringArgumentType.getString(context, "threadId"));
                        comment.delete();

                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.comment.delete", comment.content));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        ))));

        // cloudmusic comment reply id threadId content
        CloudMusic.then(Comment.then(literal("reply").then(argument("id", LongArgumentType.longArg()).then(
                argument("threadId", StringArgumentType.string()).then(
                        argument("content", StringArgumentType.string()).executes(contextData -> {
                            long id = LongArgumentType.getLong(contextData, "id");
                            JsonObject json = page.getJsonItem(jsonObject -> jsonObject.get("commentId").getAsLong() == id);
                            if (json == null) {
                                return Command.SINGLE_SUCCESS;
                            }

                            runCommand(contextData, context -> {
                                Comment comment = new Comment(music163.getHttpClient(), json, StringArgumentType.getString(context, "threadId"));
                                comment.reply(StringArgumentType.getString(context, "content"));

                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.comment.reply", comment.content));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                )))));

        // cloudmusic volume
        CloudMusic.then(Volume.executes(context -> {
            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.volume", Configs.PLAY.VOLUME.getIntegerValue()));
            return Command.SINGLE_SUCCESS;
        }));

        // cloudmusic volume volume
        CloudMusic.then(Volume.then(
                argument("volume", IntegerArgumentType.integer(0, 100)).executes(contextData -> {
                    runCommand(contextData, context -> {
                        player.volumeSet(IntegerArgumentType.getInteger(context, "volume"));
                    });
                    return Command.SINGLE_SUCCESS;
                }))
        );

        // cloudmusic lyric [on|off]
        CloudMusic.then(literal("lyric").executes(context -> {
            Configs.GUI.LYRIC.setBooleanValue(!Configs.GUI.LYRIC.getBooleanValue());
            Configs.INSTANCE.save();
            context.getSource().sendFeedback(Component.translatable(Configs.GUI.LYRIC.getBooleanValue() ? "cloudmusic.info.command.lyric.on" : "cloudmusic.info.command.lyric.off"));
            return Command.SINGLE_SUCCESS;
        }));
        CloudMusic.then(literal("lyric").then(literal("on").executes(context -> {
            Configs.GUI.LYRIC.setBooleanValue(true);
            Configs.INSTANCE.save();
            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.lyric.on"));
            return Command.SINGLE_SUCCESS;
        })));
        CloudMusic.then(literal("lyric").then(literal("off").executes(context -> {
            Configs.GUI.LYRIC.setBooleanValue(false);
            Configs.INSTANCE.save();
            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.lyric.off"));
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic musicinfo [on|off]
        CloudMusic.then(literal("musicinfo").executes(context -> {
            Configs.GUI.MUSIC_INFO.setBooleanValue(!Configs.GUI.MUSIC_INFO.getBooleanValue());
            Configs.INSTANCE.save();
            context.getSource().sendFeedback(Component.translatable(Configs.GUI.MUSIC_INFO.getBooleanValue() ? "cloudmusic.info.command.musicinfo.on" : "cloudmusic.info.command.musicinfo.off"));
            return Command.SINGLE_SUCCESS;
        }));
        CloudMusic.then(literal("musicinfo").then(literal("on").executes(context -> {
            Configs.GUI.MUSIC_INFO.setBooleanValue(true);
            Configs.INSTANCE.save();
            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.musicinfo.on"));
            return Command.SINGLE_SUCCESS;
        })));
        CloudMusic.then(literal("musicinfo").then(literal("off").executes(context -> {
            Configs.GUI.MUSIC_INFO.setBooleanValue(false);
            Configs.INSTANCE.save();
            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.musicinfo.off"));
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic page prev
        CloudMusic.then(Page.then(literal("prev").executes(context -> {
            if (page == null) {
                return Command.SINGLE_SUCCESS;
            }
            page.prev(context.getSource());
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic page next
        CloudMusic.then(Page.then(literal("next").executes(contextData -> {
            if (page == null) {
                return Command.SINGLE_SUCCESS;
            }

            runCommand(contextData, context -> {
                page.next(context.getSource());
            });
            return Command.SINGLE_SUCCESS;
        })));

        // cloudmusic page to page
        CloudMusic.then(Page.then(literal("to").then(
                argument("page", IntegerArgumentType.integer()).executes(contextData -> {
                    if (page == null) {
                        return Command.SINGLE_SUCCESS;
                    }

                    runCommand(contextData, context -> {
                        page.to(IntegerArgumentType.getInteger(context, "page") - 1, context.getSource());
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic playing
        CloudMusic.then(Playing.executes(context -> {
            player.getPlayingMusic().printToChatHud(context.getSource());
            return Command.SINGLE_SUCCESS;
        }));

        // cloudmusic playing all
        CloudMusic.then(Playing.then(literal("all").executes(context -> {
                    page = player.playingAll();
                    page.setInfoText(Component.translatable("cloudmusic.info.page.playing.all"));
                    page.look(context.getSource());
                    return Command.SINGLE_SUCCESS;
                })
        ));

        // cloudmusic login email email password
        CloudMusic.then(Login.then(literal("email").then(
                argument("email", StringArgumentType.string()).then(
                        argument("password", StringArgumentType.greedyString()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                resetCookie(loginMusic163.email(StringArgumentType.getString(context, "email"), StringArgumentType.getString(context, "password")));
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.login", my.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        })
                ))
        ));

        // cloudmusic login captcha phone
        CloudMusic.then(Login.then(literal("captcha").then(
                argument("phone", LongArgumentType.longArg()).executes(contextData -> {
                    runCommand(contextData, context -> {
                        loginMusic163.sendCaptcha(LongArgumentType.getLong(context, "phone"), Configs.LOGIN.COUNTRY_CODE.getIntegerValue());
                        context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.login.send.captcha"));
                    });
                    return Command.SINGLE_SUCCESS;
                })
        )));

        // cloudmusic login captcha phone captcha
        CloudMusic.then(Login.then(literal("captcha").then(
                argument("phone", LongArgumentType.longArg()).then(
                        argument("captcha", IntegerArgumentType.integer()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                resetCookie(loginMusic163.cellphone(LongArgumentType.getLong(context, "phone"), IntegerArgumentType.getInteger(context, "captcha"), Configs.LOGIN.COUNTRY_CODE.getIntegerValue()));
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.login", my.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
        )));

        // cloudmusic login phone phone password
        CloudMusic.then(Login.then(literal("phone").then(
                argument("phone", LongArgumentType.longArg()).then(
                        argument("password", StringArgumentType.greedyString()).executes(contextData -> {
                            runCommand(contextData, context -> {
                                resetCookie(loginMusic163.cellphone(LongArgumentType.getLong(context, "phone"), StringArgumentType.getString(context, "password"), Configs.LOGIN.COUNTRY_CODE.getIntegerValue()));
                                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.login", my.name));
                            });
                            return Command.SINGLE_SUCCESS;
                        }))
        )));

        // cloudmusic login qr
        CloudMusic.then(Login.then(literal("qr").executes(contextData -> {
            runCommand(contextData, context -> {
                String qrKey = loginMusic163.qrKey();
                loginMusic163.getQRCodeTexture(qrKey);
                try {
                    loadQRCode = true;
                    resetCookie(loginMusic163.qrLogin(qrKey));
                } catch (ActionException err) {
                    context.getSource().sendFeedback(Component.literal(err.getMessage()));
                    return;
                } finally {
                    loadQRCode = false;
                }

                context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.login", my.name));
            });
            return Command.SINGLE_SUCCESS;
        })));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LOGGER.info("[CloudMusic][Cmd] 客户端指令注册回调触发, 开始注册");
            dispatcher.register(
                    CloudMusic
                            .then(
                                    // cloudmusic stop
                                    literal("stop").executes(context -> {
                                        player.stop();
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(
                                    // cloudmusic continue
                                    literal("continue").executes(context -> {
                                        player.continues();
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(
                                    // cloudmusic prev
                                    literal("prev").executes(context -> {
                                        player.prev();
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(
                                    // cloudmusic next
                                    literal("next").executes(context -> {
                                        player.next();
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(
                                    // cloudmusic to
                                    literal("to").then(
                                            argument("index", IntegerArgumentType.integer()).executes(context -> {
                                                player.to(IntegerArgumentType.getInteger(context, "index"));
                                                return Command.SINGLE_SUCCESS;
                                            })
                                    ))
                            .then(
                                    // cloudmusic del
                                    literal("del").executes(context -> {
                                        player.deletePlayingMusic();
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(
                                    // cloudmusic trash
                                    literal("trash").executes(contextData -> {
                                        IMusic music = player.getPlayingMusic();
                                        if (!(music instanceof Music)) {
                                            return Command.SINGLE_SUCCESS;
                                        }

                                        player.deletePlayingMusic();
                                        runCommand(contextData, context -> {
                                            ((Music) music).addTrashCan();
                                            context.getSource().sendFeedback(Component.translatable("cloudmusic.info.command.trash", music.getName()));
                                        });
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(
                                    // cloudmusic random
                                    literal("random").executes(context -> {
                                        player.randomPlay();
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )
                            .then(
                                    // cloudmusic exit
                                    literal("exit").executes(context -> {
                                        resetPlayer(new ArrayList<>());
                                        return Command.SINGLE_SUCCESS;
                                    })
                            )

            );
        });
    }
}
