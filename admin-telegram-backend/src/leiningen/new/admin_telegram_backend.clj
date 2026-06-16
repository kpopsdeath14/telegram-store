(ns leiningen.new.admin-telegram-backend
  (:require [leiningen.new.templates :as tmpl]
            [leiningen.core.main :as main]))

(def render (tmpl/renderer "admin_telegram_backend"))

(defn admin-telegram-backend
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (tmpl/name-to-path name)}]
    (main/info "Generating fresh 'lein new' admin-telegram-backend project.")
    (tmpl/->files data
                  ["src/{{sanitized}}/core.clj" (render "src/core.clj" data)]
                  ["src/{{sanitized}}/datamodule.clj" (render "src/datamodule.clj" data)]
                  ["src/{{sanitized}}/app_data.clj" (render "src/app_data.clj" data)]
                  ["src/{{sanitized}}/tg_auth.clj" (render "src/tg_auth.clj" data)]
                  ["src/{{sanitized}}/services/cdek.clj" (render "src/services/cdek.clj" data)]
                  ["project.clj" (render "project.clj" data)]
                  ["app_state.edn" (render "app_state.edn" data)]
                  )
    )
  )
