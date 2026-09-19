package com.lisa.app.overlay

import com.lisa.app.domain.CapturedContent
import com.lisa.app.domain.SummaryResult

sealed interface BubbleUiState {
    /** La bulle flottante seule, repliée, prête à être déplacée ou tapée. */
    data object Collapsed : BubbleUiState

    /** "Tu veux que je résume ça ? Oui/Non" juste après un tap sur la bulle. */
    data class Prompt(val content: CapturedContent) : BubbleUiState

    /** Le moteur de résumé travaille. */
    data class Loading(val content: CapturedContent) : BubbleUiState

    /** Résumé + pertinence personnelle affichés. */
    data class Result(val content: CapturedContent, val result: SummaryResult) : BubbleUiState

    /** Rien n'a pu être lu sur l'écran courant. */
    data object NothingCaptured : BubbleUiState
}
