(ns midimap.core-test
  (:require [clojure.test :refer :all]
            [midimap.core :as core]))

(def test-events (atom []))


(defn test-setup  []
  (reset! test-events []))


(defn each-fixture  [f]
  (test-setup)
  (f))


(use-fixtures :each each-fixture)


(def dummy-note-1
  (core/note 63 :default #(swap! test-events conj %)))


(def dummy-note-2
  (core/note 64 :default #(swap! test-events conj (+ 1000 %))))


(def dummy-kit-1
  (core/kit 2 dummy-note-1 dummy-note-2))


(def dummy-event-1
  {:channel 2
   :command :note-on
   :velocity 115
   :note 63})


(def dummy-event-2
  {:channel 2
   :command :note-on
   :velocity 115
   :note 64})


(deftest create-default-note
  (is (= 63 (:id dummy-note-1)))
  (is (= false (:mute dummy-note-1))))


(deftest create-kit
  (is (= 2 (:channel dummy-kit-1)))
  (is (= dummy-note-1 (first ((:notes dummy-kit-1) 63))))
  (is (= dummy-note-2 (first ((:notes dummy-kit-1) 64)))))


(deftest fire-events-in-kit
  (core/event dummy-event-1 dummy-kit-1)
  (core/event (assoc dummy-event-1 :command :note-off)
              dummy-kit-1)
  (is (= 115 (first @test-events)))
  (is (= 0 (second @test-events))))


(deftest fire-events-in-multiple-kits
  (let [dummy-kit-2 (core/kit 2 dummy-note-2)]
    (core/event dummy-event-2
                dummy-kit-1
                dummy-kit-2)
    (is (= [1115 1115] @test-events))))


(deftest fires-events-only-in-kits-of-given-channel
  (core/event dummy-event-1
              (core/kit 1 dummy-note-1)
              (core/kit 2 dummy-note-1))
  (is (= [115] @test-events)))


(deftest doesnt-bomb-if-no-kits-match
  (core/event (assoc dummy-event-1 :channel 9)
              dummy-kit-1)
  (is (empty? @test-events)))


(deftest doesnt-bomb-if-no-notes-match
  (core/event (assoc dummy-event-1 :note 99)
              dummy-kit-1)
  (is (empty? @test-events)))
