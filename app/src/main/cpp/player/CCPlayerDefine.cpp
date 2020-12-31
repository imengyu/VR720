//
// Created by roger on 2020/12/22.
//

#include "CCPlayerDefine.h"

const char* CCVideoStateToString(CCVideoState state) {
    switch(state) {
        case CCVideoState::Loading:
            return "Loading";
        case CCVideoState::Failed:
            return "Failed";
        case CCVideoState::NotOpen:
            return "NotOpen";
        case CCVideoState::Playing:
            return "Playing";
        case CCVideoState::Ended:
            return "Ended";
        case CCVideoState::Opened:
            return "Paused";
    }
    return "";
}
const char* CCDecodeStateToString(CCDecodeState state) {
    switch(state) {
        case CCDecodeState::NotInit:
            return "NotInit";
        case CCDecodeState::Preparing:
            return "Preparing";
        case CCDecodeState::Ready:
            return "Ready";
        case CCDecodeState::Paused:
            return "Paused";
        case CCDecodeState::Finished:
            return "Finished";
        case CCDecodeState::FinishedWithError:
            return "FinishedWithError";
        case CCDecodeState::Decoding:
            return "Decoding";
        case CCDecodeState::DecodingToSeekPos:
            return "DecodingToSeekPos";
        case CCDecodeState::Finish:
            return "Finish";
    }
    return "";
}
const char* CCRenderStateToString(CCRenderState state) {
    switch(state) {
        case CCRenderState::NotRender:
            return "NotRender";
        case CCRenderState::Rendering:
            return "Rendering";
        case CCRenderState::RenderingToSeekPos:
            return "RenderingToSeekPos";
    }
    return "";
}

