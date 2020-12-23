//
// Created by roger on 2020/12/22.
//

#include "CCVideoDevice.h"

void CCVideoDevice::AVSyncAndComplete()
{
    int     tickframe, tickdiff, scdiff, avdiff = -1;
    int64_t tickcur, sysclock;

    if (!(status & VDEV_PAUSE)) {
        //++ play completed ++//
        if (completed_apts != in->apts || completed_vpts != cmnvars->vpts) {
            completed_apts = cmnvars->apts;
            completed_vpts = cmnvars->vpts;
            completed_counter = 0;
            status &=~VDEV_COMPLETED;
        } else if (!cmnvars->apktn && !cmnvars->apktn && ++completed_counter == COMPLETED_COUNTER) {
            status |= VDEV_COMPLETED;
            player_send_message(cmnvars->winmsg, MSG_PLAY_COMPLETED, 0);
        }
        //-- play completed --//

        //++ frame rate & av sync control ++//
        tickframe   = 100 * tickframe / speed;
        tickcur     = av_gettime_relative() / 1000;
        tickdiff    = (int)(tickcur - ticklast);
        ticklast = tickcur;

        sysclock= cmnvars->start_pts + (tickcur - cmnvars->start_tick) * speed / 100;
        scdiff  = (int)(sysclock - cmnvars->vpts - tickavdiff); // diff between system clock and video pts
        avdiff  = (int)(cmnvars->apts  - cmnvars->vpts - tickavdiff); // diff between audio and video pts
        avdiff  = cmnvars->apts <= 0 ? scdiff : avdiff; // if apts is invalid, sync video to system clock

        if (tickdiff - tickframe >  5) ticksleep--;
        if (tickdiff - tickframe < -5) ticksleep++;
        if (cmnvars->vpts >= 0) {
            if      (avdiff >  500) ticksleep -= 3;
            else if (avdiff >  50 ) ticksleep -= 2;
            else if (avdiff >  30 ) ticksleep -= 1;
            else if (avdiff < -500) ticksleep += 3;
            else if (avdiff < -50 ) ticksleep += 2;
            else if (avdiff < -30 ) ticksleep += 1;
        }
        if (ticksleep < 0) ticksleep = 0;
        //-- frame rate & av sync control --//
    } else {
        ticksleep = tickframe;
    }

    if (ticksleep > 0 && cmnvars->init_params->avts_syncmode != AVSYNC_MODE_LIVE_SYNC0) av_usleep(ticksleep * 1000);
    av_log(NULL, AV_LOG_INFO, "d: %3d, s: %3d\n", avdiff, ticksleep);
}