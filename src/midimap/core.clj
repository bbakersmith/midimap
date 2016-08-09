(ns midimap.core)


(defprotocol Note
  (note-on
   [self midi-event]
   ;; Returns the converted value to pass to the event handler, or nil to
   ;; prevent the event handler from firing.
   )
  (note-off
   [self midi-event]
   ;; Returns the converted value to pass to the event handler, or nil to
   ;; prevent the event handler from firing.
   ))


(defrecord DefaultNote [id event max-val min-val]
  Note
  (note-on
    ;; Return velocity scaled to range specified by :max-val
    ;; and :min-val, set to the standard midi range 0-127 by default.
    ;; A float unless both min- and max-val are integers.
    [self midi-event]
    (let [value (-> (- (:max-val self) (:min-val self))
                    (/ 127.0)
                    (* (:velocity midi-event))
                    (+ min-val))]
      (if (and (integer? (:max-val self))
               (integer? (:min-val self)))
        (int value)
        value)))
  (note-off [self midi-event]
    (:min-val self)))


(defn create-default-note [definition]
  (map->DefaultNote
   (merge {:max-val 127 :min-val 0} definition)))


;; TODO HexNote
(defrecord HexNote [id event max-val min-val]
  Note
  (note-on [self midi-event]
    ;; convert to hex
    true
    )
  (note-off [self midi-event]
    ;; min val or 0x00
    false
    ))


;; TODO OptionNote
;; array of options
;; min and max take index or value
(defrecord OptionNote [id event max-val min-val options]
  Note
  (note-on [self midi-event]
    ;; pick option
    true
    )
  (note-off [self midi-event]
    ;; min val or first
    false
    ))


(defrecord BooleanNote [id event]
  Note
  (note-on [self midi-event]
    true)
  (note-off [self midi-event]
    false))


;; TODO register-note-type function for adding constructors
(def note-constructors {
                        :boolean map->BooleanNote
                        :default create-default-note
                        :hex     map->HexNote
                        :option  map->OptionNote
                        })


(defn note
  ([id note-type event] (note id note-type event {}))
  ([id note-type event args]
  (if-let [constructor (note-constructors note-type)]
    (constructor (merge args {:id id
                              :event event}))
    (throw (Exception.
            (format "No constructor registered for '%s'" note-type))))))


(defn kit [channel & notes]
  {:channel channel
   :notes (group-by :id notes)})


(defn event [midi-event & kits]
  (let [kits (group-by :channel kits)]
    (doseq [k (kits (:channel midi-event))]
      (doseq [n ((:notes k) (:note midi-event))]
        (let [value (case (:command midi-event)
                      :note-on (note-on n midi-event)
                      :note-off (note-off n midi-event))]
          (when (not (nil? value))
            ((:event n) value)))))))
