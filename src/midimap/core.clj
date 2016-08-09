(ns midimap.core)


;; (kit 1
;;      (note 63 :default do-something)
;;      (note 64 :default do-something-else))


(defprotocol Note
  (note-on [note event])
  (note-off [note event]))


(defrecord DefaultNote [id event mute]
  Note
  (note-on [self midi-event]
    (:velocity midi-event))
  (note-off [self midi-event]
    0))


(def note-constructors (atom {:default map->DefaultNote}))


(defn note [id note-type event]
  (if-let [constructor (@note-constructors note-type)]
    (constructor {:id id
                  :event event
                  :mute  false})
    (throw (Exception.
            (format "No constructor registered for '%s'" note-type)))))


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
