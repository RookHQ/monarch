/// Replacements for the built-in strings of ``MonarchAvatarUpload``.
/// A property that is `nil` keeps the built-in English string.
public struct MonarchAvatarUploadLabels: Sendable {
    /// The button that opens the photo picker when there is no picture. Built in: "Upload photo".
    public var upload: String?
    /// The button that opens the photo picker when there is a picture. Built in: "Change photo".
    public var change: String?
    /// The button that removes the picture. Built in: "Remove photo".
    public var remove: String?
    /// The title of the crop editor. Built in: "Position your photo".
    public var editorTitle: String?
    /// The spoken name of the crop area. Built in: "Photo position".
    public var cropArea: String?
    /// The instructions of the crop area. Built in: "Drag to move the photo and pinch to zoom."
    public var cropHint: String?
    /// The label of the zoom slider. Built in: "Zoom".
    public var zoom: String?
    /// The spoken zoom. Receives the zoom in percent. Built in: "150%".
    public var zoomValue: (@Sendable (Int) -> String)?
    /// The accessibility action that moves the photo left. Built in: "Move left".
    public var moveLeft: String?
    /// The accessibility action that moves the photo right. Built in: "Move right".
    public var moveRight: String?
    /// The accessibility action that moves the photo up. Built in: "Move up".
    public var moveUp: String?
    /// The accessibility action that moves the photo down. Built in: "Move down".
    public var moveDown: String?
    /// The accessibility action that centres the photo at no zoom. Built in: "Reset position".
    public var reset: String?
    /// The button that saves the crop. Built in: "Save photo".
    public var save: String?
    /// The button that leaves the editor. Built in: "Cancel".
    public var cancel: String?
    /// Shown when saving fails. Built in: "The photo could not be saved. Try again."
    public var saveFailed: String?
    /// Shown when the picked file cannot be read as a picture. Built in: "This file can't be used as a photo."
    public var unreadable: String?
    /// Shown when the picked file is larger than allowed. Receives the limit,
    /// formatted. Built in: "Choose a photo smaller than 5 MB."
    public var tooLarge: (@Sendable (String) -> String)?

    /// Replacements for built-in strings. Every parameter is optional.
    public init(
        upload: String? = nil,
        change: String? = nil,
        remove: String? = nil,
        editorTitle: String? = nil,
        cropArea: String? = nil,
        cropHint: String? = nil,
        zoom: String? = nil,
        zoomValue: (@Sendable (Int) -> String)? = nil,
        moveLeft: String? = nil,
        moveRight: String? = nil,
        moveUp: String? = nil,
        moveDown: String? = nil,
        reset: String? = nil,
        save: String? = nil,
        cancel: String? = nil,
        saveFailed: String? = nil,
        unreadable: String? = nil,
        tooLarge: (@Sendable (String) -> String)? = nil
    ) {
        self.upload = upload
        self.change = change
        self.remove = remove
        self.editorTitle = editorTitle
        self.cropArea = cropArea
        self.cropHint = cropHint
        self.zoom = zoom
        self.zoomValue = zoomValue
        self.moveLeft = moveLeft
        self.moveRight = moveRight
        self.moveUp = moveUp
        self.moveDown = moveDown
        self.reset = reset
        self.save = save
        self.cancel = cancel
        self.saveFailed = saveFailed
        self.unreadable = unreadable
        self.tooLarge = tooLarge
    }
}
