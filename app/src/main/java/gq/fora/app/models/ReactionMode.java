package gq.fora.app.models;

import com.amrdeveloper.reactbutton.Reaction;
import gq.fora.app.R;

public final class ReactionMode {

    public static Reaction defaultReact =
            new Reaction(
                    ReactConstants.DEFAULT,
                    ReactConstants.LIKE,
                    ReactConstants.GRAY,
                    R.drawable.ic_gray_like);

    public static Reaction[] reactions = {
        new Reaction(ReactConstants.LIKE, ReactConstants.BLUE, R.drawable.ic_like),
        new Reaction(ReactConstants.LOVE, ReactConstants.RED_LOVE, R.drawable.ic_heart),
        new Reaction(ReactConstants.SMILE, ReactConstants.YELLOW_WOW, R.drawable.ic_happy),
        new Reaction(ReactConstants.WOW, ReactConstants.YELLOW_WOW, R.drawable.ic_surprise),
        new Reaction(ReactConstants.SAD, ReactConstants.YELLOW_HAHA, R.drawable.ic_sad),
        new Reaction(ReactConstants.ANGRY, ReactConstants.RED_ANGRY, R.drawable.ic_angry),
    };
}
