package video.player.qrplayer.player;

import android.content.Context;
import android.content.ContextWrapper;

public class AudioServiceLeakFix extends ContextWrapper {

	AudioServiceLeakFix(Context base) {
		super(base);
	}

	public static ContextWrapper preventLeakOf(Context base) {
		return new AudioServiceLeakFix(base);
	}

	@Override
	public Object getSystemService(String name) {
		if (Context.AUDIO_SERVICE.equals(name)) {
			return getApplicationContext().getSystemService(name);
		}
		return super.getSystemService(name);
	}
}