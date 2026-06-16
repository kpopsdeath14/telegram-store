(ns leiningen.new.admin-telegram-frontend
  (:require [leiningen.new.templates :as tmpl]
            [leiningen.core.main :as main]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def render (tmpl/renderer "admin_telegram_frontend"))

(defn- get-template-files
  "Рекурсивно получает все файлы из директории шаблонов"
  [dir-path]
  (let [dir (io/file dir-path)]
    (when (.exists dir)
      (->> (file-seq dir)
           (filter #(.isFile %))
           (map #(.getPath %))
           (map #(str/replace % (str dir-path "/") ""))
           (remove #(str/starts-with? % "."))))))

(defn- template-path->output-path
  "Преобразует путь шаблона в путь выходного файла"
  [template-path sanitized-name]
  (cond
    ;; Файлы в src/ получают префикс с именем проекта
    (str/starts-with? template-path "src/")
    (str "src/" sanitized-name "/" (str/replace template-path #"^src/" ""))

    ;; Остальные файлы сохраняют свою структуру
    :else template-path))

(defn- template-path->render-path
  "Преобразует путь шаблона в путь для рендеринга"
  [template-path]
  template-path)

(defn admin-telegram-frontend
  "FIXME: write documentation"
  [name]
  (let [data {:name name
              :sanitized (tmpl/name-to-path name)}
        template-dir "resources/leiningen/new/admin_telegram_frontend"
        template-files (get-template-files template-dir)]

    (println "Template files found:" template-files)

    (main/info "Generating fresh 'lein new' admin-telegram-frontend project.")

    (apply tmpl/->files data
           (concat
            ;; Автоматически сгенерированные файлы из шаблонов
            (map (fn [template-file]
                   (let [output-path (template-path->output-path template-file (:sanitized data))
                         render-path (template-path->render-path template-file)]
                     [output-path (render render-path data)]))
                 template-files)

            ;; Дополнительные корневые файлы (если нужно особое поведение)
            (map (fn [[output-path template-file]]
                   [output-path (render template-file data)])
                 [["shadow-cljs.edn" "shadow-cljs.edn"]
                  ["package.json" "package.json"]
                  ["public/index.html" "public/index.html"]
                  ["public/css/site.css" "public/css/site.css"]
                  ["public/config.edn" "public/config.edn"]])))))
