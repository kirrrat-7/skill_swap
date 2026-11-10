-- Skill taxonomy is reference data, not sample user content. Skill listings and members remain empty
-- until students create real records through the app.
INSERT INTO skill_categories (name, slug, icon, display_order, active, created_at, updated_at) VALUES
('Programming', 'programming', 'code-2', 10, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Artificial Intelligence', 'artificial-intelligence', 'brain', 20, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Data Science', 'data-science', 'chart-no-axes-combined', 30, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Web Development', 'web-development', 'panels-top-left', 40, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Design', 'design', 'pen-tool', 50, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Communication', 'communication', 'messages-square', 60, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Languages', 'languages', 'languages', 70, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Mathematics', 'mathematics', 'sigma', 80, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Business', 'business', 'briefcase-business', 90, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
('Creative Skills', 'creative-skills', 'lightbulb', 100, 1, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO skills (category_id, name, created_by, created_at)
SELECT c.id, s.name, NULL, UTC_TIMESTAMP(6)
FROM skill_categories c
JOIN (
    SELECT 'programming' slug, 'Java' name UNION ALL
    SELECT 'programming', 'Python' UNION ALL
    SELECT 'programming', 'C and C++' UNION ALL
    SELECT 'programming', 'Data Structures and Algorithms' UNION ALL
    SELECT 'programming', 'Git and GitHub' UNION ALL
    SELECT 'artificial-intelligence', 'Machine Learning' UNION ALL
    SELECT 'artificial-intelligence', 'Generative AI' UNION ALL
    SELECT 'artificial-intelligence', 'Prompt Engineering' UNION ALL
    SELECT 'artificial-intelligence', 'Deep Learning' UNION ALL
    SELECT 'data-science', 'SQL' UNION ALL
    SELECT 'data-science', 'Data Visualization' UNION ALL
    SELECT 'data-science', 'Excel' UNION ALL
    SELECT 'data-science', 'Statistics' UNION ALL
    SELECT 'web-development', 'HTML and CSS' UNION ALL
    SELECT 'web-development', 'JavaScript' UNION ALL
    SELECT 'web-development', 'Spring Boot' UNION ALL
    SELECT 'web-development', 'React' UNION ALL
    SELECT 'web-development', 'REST API Design' UNION ALL
    SELECT 'design', 'Figma' UNION ALL
    SELECT 'design', 'User Experience Design' UNION ALL
    SELECT 'design', 'Illustration' UNION ALL
    SELECT 'communication', 'Public Speaking' UNION ALL
    SELECT 'communication', 'Technical Writing' UNION ALL
    SELECT 'communication', 'Presentation Skills' UNION ALL
    SELECT 'languages', 'English' UNION ALL
    SELECT 'languages', 'Hindi' UNION ALL
    SELECT 'languages', 'Spanish' UNION ALL
    SELECT 'mathematics', 'Calculus' UNION ALL
    SELECT 'mathematics', 'Linear Algebra' UNION ALL
    SELECT 'business', 'Project Management' UNION ALL
    SELECT 'business', 'Entrepreneurship' UNION ALL
    SELECT 'creative-skills', 'Photography' UNION ALL
    SELECT 'creative-skills', 'Music Theory'
) s ON s.slug = c.slug;
