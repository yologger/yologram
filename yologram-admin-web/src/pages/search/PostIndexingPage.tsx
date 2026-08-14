import { App, Button, Card, Form, InputNumber, Typography } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import useIndexAllPostsMutation from '../../queries/useIndexAllPostsMutation'
import useIndexPostMutation from '../../queries/useIndexPostMutation'
import useIndexPostRangeMutation from '../../queries/useIndexPostRangeMutation'
import type { IndexingSection } from '../../apis/postIndexing'
import { getErrorMessage } from '../../lib/error'
import styles from './PostIndexingPage.module.css'

interface RangeFormValues {
  from: number
  to: number
}

interface SingleFormValues {
  id: number
}

interface Props {
  section: IndexingSection
  /** 화면 타이틀에 쓰는 섹션 한글명 */
  sectionLabel: string
}

/**
 * 게시글 검색 인덱싱 — 검색 인덱스를 다시 만드는 운영 조작.
 *
 * 세 요청 모두 SQS에 작업을 넣고 즉시 202로 끝난다(실제 색인은 worker가 비동기 수행).
 * 그래서 화면은 "발행됨"까지만 알리고 진행률은 보여주지 않는다 —
 * 큐 깊이를 노출하는 엔드포인트가 아직 없다. 필요해지면 상태 조회 API를 추가한다.
 *
 * 섹션을 prop으로 받아 tech·invest·politics가 같은 화면을 공유한다
 * (경로만 다르고 조작이 동일하다). 현재 백엔드는 tech만 구현돼 나머지는 라우트에서 ComingSoon으로 막는다.
 */
export default function PostIndexingPage({ section, sectionLabel }: Props) {
  const { message, modal } = App.useApp()
  const [rangeForm] = Form.useForm<RangeFormValues>()
  const [singleForm] = Form.useForm<SingleFormValues>()

  const allMutation = useIndexAllPostsMutation(section)
  const rangeMutation = useIndexPostRangeMutation(section)
  const singleMutation = useIndexPostMutation(section)

  const notifyPublished = (detail: string) => {
    message.success(`인덱싱 작업을 발행했습니다 (${detail}). 색인은 잠시 후 반영됩니다.`)
  }

  // 전체 인덱싱은 게시글 수만큼 메시지가 발행돼 DB·OpenSearch에 부하가 간다 — 확인을 받는다
  const confirmIndexAll = () => {
    modal.confirm({
      title: '전체 인덱싱을 실행할까요?',
      content: `${sectionLabel} 게시글을 모두 다시 색인합니다. 게시글 수에 비례해 시간이 걸립니다.`,
      okText: '실행',
      cancelText: '취소',
      onOk: () =>
        allMutation.mutateAsync().then(
          () => notifyPublished('전체'),
          (error) => {
            message.error(getErrorMessage(error))
            throw error
          }
        ),
    })
  }

  const submitRange = (values: RangeFormValues) => {
    rangeMutation.mutate(values, {
      onSuccess: () => notifyPublished(`${values.from} ~ ${values.to}`),
      onError: (error) => message.error(getErrorMessage(error)),
    })
  }

  const submitSingle = (values: SingleFormValues) => {
    singleMutation.mutate(values.id, {
      onSuccess: () => notifyPublished(`id ${values.id}`),
      onError: (error) => message.error(getErrorMessage(error)),
    })
  }

  return (
    <div>
      <div className={styles.header}>
        <Typography.Title level={4} className={styles.title}>
          {sectionLabel} 게시글 인덱싱
        </Typography.Title>
        <div className={styles.description}>
          검색 인덱스를 다시 만듭니다. 요청은 큐에 적재되고 색인은 백그라운드에서 수행됩니다.
        </div>
      </div>

      <div className={styles.cards}>
        <Card title="전체 인덱싱" size="small">
          <Button
            type="primary"
            icon={<ReloadOutlined />}
            loading={allMutation.isPending}
            onClick={confirmIndexAll}
          >
            전체 인덱싱 실행
          </Button>
        </Card>

        <Card title="범위 인덱싱" size="small">
          <Form form={rangeForm} onFinish={submitRange} className={styles.inline}>
            <Form.Item
              name="from"
              rules={[{ required: true, message: '시작 id를 입력하세요.' }]}
              aria-label="시작 id"
            >
              <InputNumber min={1} placeholder="시작 id" />
            </Form.Item>
            <span className={styles.rangeSeparator}>~</span>
            <Form.Item name="to" rules={[{ required: true, message: '끝 id를 입력하세요.' }]}>
              <InputNumber min={1} placeholder="끝 id" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={rangeMutation.isPending}>
                범위 인덱싱 실행
              </Button>
            </Form.Item>
          </Form>
        </Card>

        <Card title="단건 인덱싱" size="small">
          <Form form={singleForm} onFinish={submitSingle} className={styles.inline}>
            <Form.Item name="id" rules={[{ required: true, message: '게시글 id를 입력하세요.' }]}>
              <InputNumber min={1} placeholder="게시글 id" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" htmlType="submit" loading={singleMutation.isPending}>
                단건 인덱싱 실행
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </div>
    </div>
  )
}
