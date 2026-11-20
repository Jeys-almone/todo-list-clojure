(ns todo.frontend.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [clojure.string :as str]))

;; Estado da aplicação: um mapa dentro de um atom reativo
(defonce app-state (r/atom {:next-id    1
                            :input-text ""
                            :todos      []}))

(defn adicionar-todo-local []
  (let [texto (:input-text @app-state)]
    (when-not (str/blank? texto)
      (swap! app-state
             (fn [estado]
               (-> estado
                   (update :todos conj {:id        (:next-id estado)
                                        :title     texto
                                        :completed false})
                   (update :next-id inc)
                   (assoc :input-text "")))))))

(defn todo-form []
  [:div
   [:input {:type        "text"
            :placeholder "O que precisa ser feito?"
            :value       (:input-text @app-state)
            :on-change   #(swap! app-state assoc :input-text (-> % .-target .-value))}]
   [:button {:on-click #(adicionar-todo-local)} "Adicionar"]])

(defn todo-list []
  [:ul
   (for [todo (:todos @app-state)]
     ^{:key (:id todo)} [:li (:title todo)])])

(defn app []
  [:div
   [:h1 "Meu Todo List"]
   [todo-form]
   [todo-list]])

(defonce root (rdom/create-root (js/document.getElementById "app")))

(defn ^:dev/after-load init []
  (rdom/render root [app]))