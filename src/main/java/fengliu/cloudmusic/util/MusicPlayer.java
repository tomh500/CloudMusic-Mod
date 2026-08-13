package fengliu.cloudmusic.util;

import fengliu.cloudmusic.CloudMusicClient;
import fengliu.cloudmusic.config.Configs;
import fengliu.cloudmusic.music163.ActionException;
import fengliu.cloudmusic.music163.IMusic;
import fengliu.cloudmusic.music163.Lyric;
import fengliu.cloudmusic.music163.data.DjMusic;
import fengliu.cloudmusic.music163.data.Music;
import fengliu.cloudmusic.render.MusicIconTexture;
import fengliu.cloudmusic.util.page.Page;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.network.chat.Component;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * 歌曲播放对象
 */
public class MusicPlayer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("cloudmusic");
    private final Minecraft client = Minecraft.getInstance();
    protected final List<IMusic> playList;
    private IMusic playingMusic = null;
    private SourceDataLine play;
    private Lyric lyric;
    protected int playIn = 0;
    protected int playListSize;
    protected volatile boolean loopPlayIn = true;
    protected volatile boolean notExitFlag = true;
    private volatile boolean load;
    private int volumePercentage;
    private volatile long playingProgress;
    private long startPlayingTime;
    private volatile long seekTargetMs = -1;
    private File playFile = null;
    private String playUrl = null;

    /**
     * 歌曲播放对象
     *
     * @param playList 歌曲列表
     */
    public MusicPlayer(List<IMusic> playList) {
        this.playListSize = playList.size();
        this.playList = playList;
        if (Configs.ENABLE.PLAY_AUTO_RANDOM.getBooleanValue()) {
            this.randomPlay();
        }

        this.volumeSet(Configs.PLAY.VOLUME.getIntegerValue());
    }

    public boolean isPlaying() {
        return this.loopPlayIn && this.load && this.getPlayingMusic() != null;
    }

    @Override
    public void run() {
        while (this.notExitFlag) {
            while (this.loopPlayIn) {
                for (; playIn < this.playListSize; this.playIn++) {
                    playMusic();

                    if (!this.loopPlayIn) {
                        break;
                    }

                    if (this.playIn == this.playListSize - 1 && !Configs.PLAY.PLAY_LOOP.getBooleanValue()) {
                        this.loopPlayIn = false;
                    }
                }

                if (!this.loopPlayIn) {
                    break;
                }
                this.playIn = 0;
            }

            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * 启动歌曲播放
     */
    public void start() {
        LOGGER.info("[CloudMusic][Player] 启动播放线程");
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("CloudMusicPlayer thread");
        thread.start();
    }

    /**
     * 播放歌曲
     */
    protected void playMusic() {
        LOGGER.info("[CloudMusic][Player] 开始播放曲目, 播放列表大小={}", this.playListSize);
        // 开始播放的时候停止所有的声音(只会停止一瞬间)
        client.getSoundManager().stop();
        IMusic music = this.playList.get(this.playIn);

        String musicUrl;
        try {
            musicUrl = music.getPlayUrl();
        } catch (ActionException err) {
            Minecraft client = Minecraft.getInstance();
            LOGGER.info("[CloudMusic][Player] 获取播放地址失败", err);
            if (client.player != null) {
                client.execute(() -> client.player.sendSystemMessage(Component.literal(err.getMessage())));
            }
            this.stop();
            return;
        }

        if (music instanceof Music aMusic) {
            if (aMusic.freeTrialInfo != null && this.client.player != null) {
                Component freeTrialMessage = Component.translatable(
                        "cloudmusic.info.play.free.trial",
                        music.getName(),
                        aMusic.freeTrialInfo.get("start").getAsInt(),
                        aMusic.freeTrialInfo.get("end").getAsInt()
                );
                this.client.execute(() -> this.client.player.sendSystemMessage(freeTrialMessage));
            }
        }

        MusicIconTexture.getMusicIcon(music);
        if (music instanceof Music) {
            this.lyric = ((Music) music).lyric();
        } else {
            this.lyric = null;
        }

        this.playingMusic = music;
        if (!Configs.PLAY.PLAY_URL.getBooleanValue()) {
            String[] urls = musicUrl.split("\\.");
            String fileType = urls[urls.length - 1];

            File file;
            if (music instanceof DjMusic) {
                file = HttpClient.download(musicUrl, CloudMusicClient.cacheHelper.getWaitCacheFile("djmusic_" + music.getId() + "." + fileType));
            } else {
                file = HttpClient.download(musicUrl, CloudMusicClient.cacheHelper.getWaitCacheFile(music.getId() + "." + fileType));
            }

            CloudMusicClient.cacheHelper.addUseSize(file);
            Component nowPlayingMessage = Component.translatable("record.nowPlaying", music.getName());
            this.client.execute(() -> this.client.gui.hud.setOverlayMessage(nowPlayingMessage, false));
            this.play(file);
        } else {
            Component nowPlayingMessage = Component.translatable("record.nowPlaying", music.getName());
            this.client.execute(() -> this.client.gui.hud.setOverlayMessage(nowPlayingMessage, false));
            this.play(musicUrl);
        }
    }

    /**
     * 播放歌曲
     */
    private void play(AudioInputStream audioInputStream) throws IOException, InterruptedException, LineUnavailableException {
        AudioFormat audioFormat = audioInputStream.getFormat();

        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        play = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
        play.open(audioFormat);
        //设置音量
        this.volumeSet(volumePercentage);

        play.start();
        if (lyric != null) {
            this.lyric.start();
        }

        int count;
        byte[] tempBuff = new byte[1024];

        this.load = true;
        this.startPlayingTime = System.currentTimeMillis();
        while ((count = audioInputStream.read(tempBuff, 0, tempBuff.length)) != -1) {
            synchronized (this) {
                while (!load)
                    wait();
            }

            long seekTarget = this.seekTargetMs;
            if (seekTarget >= 0) {
                this.seekTargetMs = -1;
                try {
                    audioInputStream.close();
                    audioInputStream = this.seekStream(seekTarget);
                    play.flush();
                    this.startPlayingTime = System.currentTimeMillis() - seekTarget;
                    this.playingProgress = seekTarget;
                } catch (Exception err) {
                    err.printStackTrace();
                    try {
                        audioInputStream = this.openAudioInputStream();
                        play.flush();
                        this.startPlayingTime = System.currentTimeMillis();
                        this.playingProgress = 0;
                    } catch (Exception err2) {
                        break;
                    }
                }
                continue;
            }

            play.write(tempBuff, 0, count);
            this.playingProgress = System.currentTimeMillis() - this.startPlayingTime;
        }

        this.playingProgress = 0;
        if (lyric != null) {
            this.lyric.exit();
        }
    }

    /**
     * 打开并解码当前播放源 (本地文件或 URL)
     */
    private AudioInputStream openAudioInputStream() throws Exception {
        AudioInputStream stream;
        if (this.playFile != null) {
            stream = AudioSystem.getAudioInputStream(this.playFile);
        } else if (this.playUrl != null) {
            stream = AudioSystem.getAudioInputStream(AudioSystem.getAudioInputStream(new URL(this.playUrl)));
        } else {
            throw new IllegalStateException("没有可播放的音频源");
        }

        AudioFormat sourceFormat = stream.getFormat();
        // 转换文件编码
        if (sourceFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
            System.out.println(sourceFormat.getEncoding());
            AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
            stream = AudioSystem.getAudioInputStream(pcmFormat, stream);
        }

        return stream;
    }

    /**
     * 重新打开播放源并跳转到指定毫秒位置
     */
    private AudioInputStream seekStream(long targetMs) throws Exception {
        AudioInputStream stream = this.openAudioInputStream();
        AudioFormat format = stream.getFormat();
        long bytesPerSecond = (long) (format.getFrameRate() * format.getFrameSize());
        if (bytesPerSecond <= 0) {
            return stream;
        }

        // 不能用 skip(): mp3spi 等解码后的转换流上 skip() 按压缩源字节跳过,
        // 会大幅越过目标位置直接读到 EOF, 导致 seek 后当前歌曲结束/自动切歌。
        // 改为从头部读取并丢弃 PCM 字节, 精确停在目标位置。
        long bytesToSkip = (long) (targetMs / 1000.0 * bytesPerSecond);
        byte[] buffer = new byte[65536];
        long skipped = 0;
        while (skipped < bytesToSkip) {
            int read = stream.read(buffer, 0, (int) Math.min(buffer.length, bytesToSkip - skipped));
            if (read <= 0) {
                break;
            }
            skipped += read;
        }
        return stream;
    }

    /**
     * 跳转到指定播放位置 (毫秒)
     *
     * @param ms 目标毫秒
     */
    public void seek(long ms) {
        if (this.playingMusic == null || this.play == null) {
            return;
        }

        long durationMs = this.playingMusic.getDurationSecond() * 1000L;
        this.seekTargetMs = Math.max(0, Math.min(ms, durationMs));
    }

    /**
     * 通过 URL 播放歌曲
     *
     * @param url 歌曲 url
     */
    private void play(String url) {
        try {
            this.playFile = null;
            this.playUrl = url;
            this.play(this.openAudioInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过文件对象播放歌曲
     *
     * @param file 文件对象
     */
    private void play(File file) {
        try {
            this.playUrl = null;
            this.playFile = file;
            this.play(this.openAudioInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置音量增益
     *
     * @param volume 音量百分比
     */
    public void volumeSet(int volume) {
        if (volume < 0) {
            volume = 0;
        }

        if (volume > 100) {
            volume = 100;
        }

        this.volumePercentage = volume;
        if (this.play == null) {
            return;
        }

        FloatControl gainControl = (FloatControl) this.play.getControl(FloatControl.Type.MASTER_GAIN);
        float minGain = gainControl.getMinimum();
        float maxGain = gainControl.getMaximum();
        // 人耳对分贝是对数感知: 之前 min*(1-v/100) 是分贝线性, 50~60 音量相当于 -32~-40dB 几乎听不见。
        // 改用平方曲线把低音量段抬高, 让音量条上的数值接近实际听感
        float t = volume / 100.0f;
        float gain = maxGain - (maxGain - minGain) * (1 - t) * (1 - t);
        gainControl.setValue(gain);

        Configs.PLAY.VOLUME.setIntegerValue(this.volumePercentage);
        Configs.INSTANCE.save();
    }

    public void volumeAdd() {
        this.volumeSet(++this.volumePercentage);
    }

    public void volumeDown() {
        this.volumeSet(--this.volumePercentage);
    }

    /**
     * 获取音量百分比
     * @return 音量百分比
     */
    public int getVolumePercentage() {
        return volumePercentage;
    }

    /**
     * 获取当前滚动到的歌词
     *
     * @return 歌词
     */
    public String[] getLyric() {
        if (lyric == null) {
            return new String[]{};
        }
        return lyric.getToLyric();
    }

    /**
     * 播放下一首
     */
    public void next() {
        if (this.playList.isEmpty()) {
            return;
        }

        this.play.stop();
        this.play.close();
    }

    /**
     * 播放上一首
     */
    public void prev() {
        this.playIn -= 2;
        if (this.playIn < -1) {
            this.playIn = -1;
        }

        next();
    }

    /**
     * 跳转至...首播放
     *
     * @param in 歌曲序号 (索引加一)
     */
    public void to(int in) {
        in -= 1;
        if (in < 0) {
            in = 0;
        }

        int maxIndex = this.playList.size() - 1;
        if (in > maxIndex) {
            in = maxIndex;
        }

        this.playIn = in - 1;
        next();
    }

    /**
     * 退出播放
     */
    public void exit() {
        if (this.lyric != null) {
            this.lyric.continues();
            this.lyric.exit();
        }

        this.loopPlayIn = false;
        this.notExitFlag = false;
        next();
    }

    /**
     * 停止播放
     */
    public void stop() {
        LOGGER.info("[CloudMusic][Player] 停止播放");
        if (this.lyric != null) {
            this.lyric.stop();
        }

        synchronized (this) {
            this.load = false;
            notifyAll();
        }
        this.loopPlayIn = false;
    }

    /**
     * 切换播放/暂停 (点击专辑封面等场景使用)
     */
    public void switchPlay() {
        if (this.playingMusic == null || !this.notExitFlag) {
            return;
        }

        if (this.isPlaying()) {
            this.stop();
        } else {
            this.continues();
        }
    }

    /**
     * 继续播放
     */
    public void continues() {
        if (this.lyric != null) {
            this.lyric.continues();
        }
        this.startPlayingTime = System.currentTimeMillis() - this.playingProgress;

        synchronized (this) {
            this.load = true;
            notifyAll();
        }
        this.loopPlayIn = true;
    }

    /**
     * 从播放列表中删除当前播放歌曲
     */
    public void deletePlayingMusic() {
        if (this.playListSize == 0) {
            if (this.playList.isEmpty()) {
                return;
            }

            this.playListSize = this.playList.size();
        }

        this.playList.remove(this.getPlayingMusic());
        this.playListSize -= 1;
        this.playIn -= 1;
        this.next();
    }

    /**
     * 正在播放
     *
     * @return 歌曲对象
     */
    public IMusic getPlayingMusic() {
        return this.playingMusic;
    }

    /**
     * 获取播放进度 (毫秒)
     *
     * @return 毫秒
     */
    public long getPlayingProgress() {
        return this.playingProgress;
    }

    /**
     * 获取播放进度 (秒)
     *
     * @return 秒
     */
    public int getPlayingProgressSecond() {
        return (int) (this.playingProgress / 1000);
    }

    /**
     * 获取播放进度 (字符串)
     *
     * @return 播放进度字符串 (格式 "分:秒")
     */
    public String getPlayingProgressToString() {
        return Time.secondToString(this.getPlayingProgressSecond());
    }

    /**
     * 播放队列
     *
     * @return 页对象
     */
    public Page playingAll() {
        return new Page(this.playList) {

            @Override
            protected TextClickItem putPageItem(Object data) {
                if (data instanceof Music music) {
                    return new TextClickItem(
                            Component.literal("§b%s §r§7 - %s".formatted(music.name, Music.getArtistsName(music.artists))),
                            Component.translatable(IdUtil.getShowInfo("page.player.to"), music.name),
                            "/cloudmusic to " + (this.limit * this.pageIn + this.data.get(this.pageIn).indexOf(data) + 1)
                    );
                }

                if (data instanceof DjMusic music) {
                    return new TextClickItem(
                            Component.literal("§b%s §r§7 - %s".formatted(music.name, music.dj.get("nickname").getAsString())),
                            Component.translatable(IdUtil.getShowInfo("page.player.to"), music.name),
                            "/cloudmusic to " + (this.limit * this.pageIn + this.data.get(this.pageIn).indexOf(data) + 1)
                    );
                }

                return null;
            }

        };
    }

    /**
     * 将播放队列随机并重新播放
     */
    public void randomPlay() {
        Collections.shuffle(this.playList);
        if (!this.isPlaying()) {
            return;
        }

        this.playIn -= 1;
        this.next();
    }

}
