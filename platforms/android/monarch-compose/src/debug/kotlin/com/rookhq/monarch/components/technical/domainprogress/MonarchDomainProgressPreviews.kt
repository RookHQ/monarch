package com.rookhq.monarch.components.technical.domainprogress

import androidx.compose.runtime.Composable
import com.rookhq.monarch.internal.preview.MonarchPreviews
import com.rookhq.monarch.internal.preview.PreviewFrame

private const val Now = 1_772_636_400_000L
private const val TwoMinutes = 120_000L

@MonarchPreviews
@Composable
internal fun MonarchDomainProgressWaitingPreview() {
    PreviewFrame {
        MonarchDomainProgress(
            host = "book.example.com",
            stage = MonarchDomainStage.Dns,
            problems = listOf(MonarchDomainProblem("Your domain points somewhere else. Remove the old A record.")),
            dnsFound = MonarchDomainDnsSnapshot(a = listOf("203.0.113.10")),
            checkedAt = Now - TwoMinutes,
            now = Now,
        )
    }
}

@MonarchPreviews
@Composable
internal fun MonarchDomainProgressCertificatePreview() {
    PreviewFrame {
        MonarchDomainProgress(
            host = "book.example.com",
            stage = MonarchDomainStage.Certificate,
            problems = listOf(
                MonarchDomainProblem("Certificate pending.", severity = MonarchDomainProblemSeverity.Info),
            ),
            notify = MonarchDomainNotify.Email,
        )
        MonarchDomainProgress(host = "book.example.com", stage = MonarchDomainStage.Live)
    }
}
