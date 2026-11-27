/// Built-in strings of the domain progress, from the `MonarchDomainProgress` strings table.
enum MonarchDomainProgressStrings {
    private static let table = "MonarchDomainProgress"

    /// A string without arguments.
    static func string(_ key: String) -> String {
        MonarchStrings.string("monarch.domainProgress.\(key)", table: table)
    }

    /// A string with arguments.
    static func string(_ key: String, _ arguments: any CVarArg...) -> String {
        String(format: string(key), arguments: arguments)
    }

    /// The built-in title of a step.
    static func title(_ step: MonarchDomainStep, state: MonarchDomainStepState, stage: MonarchDomainStage) -> String {
        switch step {
        case .dns:
            if state == .done { return string("dnsTitleDone") }
            return stage == .ownership ? string("dnsTitleOwnership") : string("dnsTitleWaiting")
        case .certificate:
            return state == .done ? string("certificateTitleDone") : string("certificateTitle")
        case .live:
            return string("liveTitle")
        }
    }

    /// The built-in detail of a step.
    static func detail(_ step: MonarchDomainStep, state: MonarchDomainStepState, host: String) -> String {
        switch (step, state) {
        case (.dns, .done): string("dnsDetailDone")
        case (.dns, _): string("dnsDetailWaiting")
        case (.certificate, .done): string("certificateDetailDone")
        case (.certificate, .todo): string("certificateDetailTodo")
        case (.certificate, _): string("certificateDetailCurrent")
        case (.live, .done): string("liveDetailDone", host)
        case (.live, .current): string("liveDetailCurrent", host)
        case (.live, _): string("liveDetailTodo", host)
        }
    }

    /// The spoken name of a state.
    static func state(_ state: MonarchDomainStepState) -> String {
        switch state {
        case .done: string("stateDone")
        case .current: string("stateCurrent")
        case .error: string("stateError")
        case .todo: string("stateTodo")
        }
    }

    /// The "keeps checking" sentence.
    static func rechecking(_ notify: MonarchDomainNotify?) -> String {
        switch notify {
        case .email: string("recheckingEmail")
        case .inApp: string("recheckingInApp")
        case nil: string("rechecking")
        }
    }
}
