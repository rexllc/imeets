package gq.fora.app.notify.exceptions;

import android.content.res.Resources.NotFoundException;

import gq.fora.app.notify.Notify;

public class NotifyDefaultChannelInfoNotFoundException extends NotFoundException {
    public NotifyDefaultChannelInfoNotFoundException(){}
    @Override
    public String getMessage() {
        return "One or more of the next values is missing from string resources: " +
                Notify.DefaultChannelKeys.ID+", " +
                Notify.DefaultChannelKeys.NAME+" or " +
                Notify.DefaultChannelKeys.DESCRIPTION;
    }
}
