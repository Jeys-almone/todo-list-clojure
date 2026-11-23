(ns todo.frontend.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [clojure.string :as str]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]))

;; Endereço do backend
(def api-url "http://localhost:3000/api")

;; Estado: agora os todos vêm da API, então não precisamos mais do :next-id
(defonce app-state (r/atom {:input-text ""
                            :todos      []}))

;; Faz a requisição e converte o JSON da resposta em mapa Clojure
(defn fetch-json [url options]
  (-> (js/fetch url (clj->js options))
      (.then (fn [resposta]
               (if (.-ok resposta)
                 (.json resposta)
                 (throw (js/Error. (str "Erro HTTP " (.-status resposta)))))))
      (.then (fn [dados]
               (js->clj dados :keywordize-keys true)))))

;; GET /api/todos
(defn get-todos []
  (swap! app-state assoc :loading true :error nil)
  (go
    (try
      (let [resposta (<p! (fetch-json (str api-url "/todos") {:method "GET"}))]
        (swap! app-state assoc :todos (:todos resposta) :loading false))
      (catch js/Error e
        (swap! app-state assoc :error (.-message (or (ex-cause e) e)) :loading false)))))

;; POST /api/todos e depois recarrega a lista
(defn create-todo [todo-data]
  (swap! app-state assoc :loading true :error nil)
  (go
    (try
      (<p! (fetch-json (str api-url "/todos")
                       {:method  "POST"
                        :headers {"Content-Type" "application/json"}
                        :body    (js/JSON.stringify (clj->js todo-data))}))
      (get-todos)
      (catch js/Error e
        (swap! app-state assoc :error (.-message (or (ex-cause e) e)) :loading false)))))

(defn toggle-todo
  "Chama a API para alternar o status de um todo."
  [id]
  (go
    (try
      (<p! (fetch-json (str api-url "/todos/" id "/toggle")
                       {:method "POST"}))
      ;; Se funcionou, recarrega a lista inteira
      (get-todos)
      (catch js/Error e
        (swap! app-state assoc :error (.-message e) :loading false)))))

(defn delete-todo
  "Chama a API para remover um todo."
  [id]
  (go
    (try
      (<p! (fetch-json (str api-url "/todos/" id)
                       {:method "DELETE"}))
      ;; Se funcionou, recarrega a lista
      (get-todos)
      (catch js/Error e
        (swap! app-state assoc :error (.-message e) :loading false)))))

(defn todo-form []
  [:div.todo-input
   [:input {:type        "text"
            :placeholder "O que precisa ser feito?"
            :value       (:input-text @app-state)
            :on-change   #(swap! app-state assoc :input-text (-> % .-target .-value))}]
   [:button {:on-click (fn []
                         (let [texto (:input-text @app-state)]
                           (when-not (str/blank? texto)
                             (create-todo {:title texto})
                             (swap! app-state assoc :input-text ""))))}
    "Adicionar"]])

(defn todo-list []
  [:ul.todo-list
   (for [todo (:todos @app-state)]
     ^{:key (:todos/id todo)}

     ;; 1. Classe CSS 'completed' quando o status for 1
     [:li.todo-item {:class (when (= 1 (:todos/completed todo)) "completed")}

      ;; 2. O Checkbox
      [:input.todo-checkbox
       {:type "checkbox"
        ;; 3. A CONVERSÃO: 0/1 -> booleano de verdade
        :checked (not= 0 (:todos/completed todo))
        ;; 4. O evento chama nossa nova função de API
        :on-change #(toggle-todo (:todos/id todo))}]

      ;; 5. O título (agora dentro de um span, para o CSS)
      [:span.todo-title (:todos/title todo)]
      
      ;; 6. Botão de delete com confirmação
      [:button.delete-btn
       {:on-click #(when (js/confirm "Remover esta tarefa?")
                     (delete-todo (:todos/id todo)))}
       "X"]])])

(defn app []
  [:div.todo-app
   [:h1 "Meu Todo List"]
   [:p "Conectado à API em " api-url]
   (when-let [erro (:error @app-state)]
     [:p {:style {:color "red"}} "Erro: " erro])
   [todo-form]
   [todo-list]])

(defonce root (rdom/create-root (js/document.getElementById "app")))

(defn ^:dev/after-load init []
  (rdom/render root [app])
  (get-todos))