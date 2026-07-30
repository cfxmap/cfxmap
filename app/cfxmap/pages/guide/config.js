export const configInfo = {
          "zh-CN": {
            'name': 'Start Conflux',
            'eco': '服务指引',
            'tool': '工具导航',
            'heroTitle': 'Conflux 生态服务指南',
            'heroSubtitle': '一站式汇集链上生态与实用工具，帮助你快速了解并使用 Conflux 生态服务',
            'heroBtnText': '立即开始',
            'firstTitle': '资金服务指引',
            'firstSubtitle': '集中展示 Conflux 生态中与资产流转相关的常见路径与工具，帮助用户顺畅、安全地完成资金转换和链上资产管理。',
            'secondTitle': '生态工具导航',
            'secondSubtitle': '整理 Conflux 生态内常用的应用与工具入口，方便用户访问链上应用并探索更多生态资源。',
            'contentList': [
              {
                title: '链上资产',
                id: 'deposit',
                content: [
                  // {
                  //   title: '人民币 → 交易所中的 USDT',
                  //   id: 'deposit-1',
                  //   list: [
                  //     {
                  //       title: '币安',
                  //       desc: '',
                  //       showDesc:false,
                  //       badge: '',
                  //       explain:'币安是全球交易量长期领先的中心化交易所之一，流动性充足、币种丰富，是许多用户购买 USDT 的主要选择。',
                  //       btnList: [
                  //         {
                  //           text: '币安初学者指南',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.binance.com/zh-CN/support/faq/detail/c780097f75dd450a82d17f1e84153276'
                  //         },
                  //         {
                  //           text: '币安 C2C 教程',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.binance.com/zh-CN/support/faq/detail/384c0a3441b04a9cbe97c9687ef86b60'
                  //         }
                  //       ]
                  //     },
                  //     {
                  //       title: 'OKX',
                  //       desc: '',
                  //       showDesc:false,
                  //       badge: '',
                  //       explain:'OKX 是全球用户覆盖最广的中心化交易所之一，提供稳定的交易体验，也是用户购买 USDT 时最常使用的渠道之一。',
                  //       btnList: [
                  //         {
                  //           text: 'OKX新手攻略',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.okx.com/zh-hans/help/okx-beginners-guide'
                  //         }
                  //       ]
                  //     }
                  //   ]
                  // },
                  {
                    title: '交易所 USDT → Conflux 链上的 USDT',
                    id: 'deposit-2',
                    list: [
                      {
                        title: 'Meson',
                        desc: '支持交易所一键充提 Conflux eSpace 的 USDT0 / USDC / AxCNH，<span class="rate">0 手续费</span>。',
                        showDesc:true,
                        badge: '',
                        explain: 'Meson 是目前 Conflux 链上使用最多的跨链桥，已支持 50+ 条主流高性能区块链。Meson 现已支持用户直接在交易所充提 Conflux eSpace 上的 USDT。目前支持的交易所有：Binance, OKX, Bybit，Gate.io 等。',
                        btnList: [
                          {
                            text: '去跨链',
                            class: 'primary',
                            alert: false,
                            link: 'https://meson.fi/'
                          }, {
                            text: '跨链教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/meson/21482?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }, {
                            text: '交易所充提教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/meson-conflux-espace-usdt/22087?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]
                      }
                    ]
                  },
                  {
                    title: 'Conflux 链上的 USDT → AxCNH',
                    id: 'deposit-3',
                    list: [
                      {
                        title: 'WallFreeX',
                        desc: '支持 Conflux 链上 USDT、CFX、AxCNH 之间的兑换。',
                        showDesc:false,
                        badge: '',
                        explain: 'Uniswap V3 机制的 DEX，滑点更低，USDT <> AxCNH 兑换更推荐使用。',
                        btnList: [
                          {
                            text: '去兑换',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/swap'
                          }
                        ]
                      }
                    ]
                  },
                  {
                    title: 'Conflux 链上的 USDT → CFX',
                    id: 'deposit-4',
                    list: [
                      {
                        title: 'Swappi',
                        desc: '支持 Conflux 链上各种资产的兑换。',
                        showDesc:false,
                        badge: '',
                        explain: 'Conflux eSpace 的首个 DEX，提供快速、低成本的代币兑换和流动性挖矿服务。',
                        btnList: [
                          {
                            text: '去兑换',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.swappi.io/#/swap'
                          }
                        ]
                      },
                      {
                        title: 'WallFreeX',
                        desc: '支持 Conflux 链上USDT、CFX、AxCNH 之间的兑换。',
                        showDesc:false,
                        badge: '',
                        explain: 'Uniswap V3 机制的 DEX，滑点更低，USDT <> AxCNH 兑换更推荐使用。',
                        btnList: [
                          {
                            text: '去兑换',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/swap'
                          }
                        ]
                      }
                    ]
                  }
                ],
              },
              {
                title: '资产兑换',
                id: 'withdraw',
                content: [
                  {
                    title: 'to C',
                    id: 'withdraw-1',
                    list: [
                      {
                        title: 'BitUnion',
                        desc: '一键将USDT转换为法币。',
                        showDesc:false,
                        badge: '',
                        explain: 'BitUnion 是 Conflux 链上的 PayFi 解决方案，致力于协助数字资产实现便捷流通与广泛应用。',
                        btnList: [
                          {
                            text: '全球速汇',
                            class: 'primary',
                            alert: false,
                            link: 'https://bitunion.io/'
                          },
                          {
                            text: '使用教程',
                            class: '',
                            alert: false,
                            link: 'https://bitunion.gitbook.io/bitunion-docs'
                          }
                        ]
                      }
                    ]
                  },
                  // {
                  //   title: 'to B',
                  //   id: 'withdraw-2',
                  //   list: [
                  //     {
                  //       title: '大额贸易',
                  //       desc: '将 AxCNH 转换为香港银行账户的离岸人民币。',
                  //       showDesc:false,
                  //       badge: '',
                  //       line:1,
                  //       explain: '汇率：1:1，手续费：<span class="rate">0.3%-0.5%</span>，提供贸易背景，形式发票',
                  //       btnList: [
                  //         {
                  //           text: '去合作',
                  //           class: 'primary',
                  //           alert: false,
                  //           link: 'https://docs.google.com/forms/d/e/1FAIpQLSd0iLl4zKRfw8RRuGXNDLkvjx_eRnCHVnV0HBThGyeteP7SuQ/viewform?usp=dialog'
                  //         }
                  //       ]
                  //     }
                  //   ]
                  // }
                ]
              },
              {
                title: '理财',
                id: 'yield',
                content: [
                  {
                    title: 'PoS 矿池质押',
                    id: 'yield-1',
                    list: [
                      {
                        title: 'CFX 质押（ABC 矿池）<span class="rate">年化收益率：8-9%</span>',
                        desc: '锁仓期为 13+1，奖励<span class="rate"> 随时可领取</span>。',
                        showDesc:false,
                        badge: '',
                        explain: 'ABC 矿池是 Conflux 链上最大的 PoS 矿池 。PoS 质押为币本位理财，主要风险来自币价波动，单次质押量需为 1000 CFX 的整数倍，ABC 矿池将收取奖励的 <span class="rate">8%</span> 作为手续费。',
                        btnList: [
                          {
                            text: '去质押',
                            class: 'primary',
                            alert: false,
                            link: 'https://confluxpos.cn/'
                          },
                          {
                            text: '质押教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/abcpool/18125?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]

                      }
                    ]
                  },
                  {
                    title: 'RWA Vault',
                    id: 'yield-2',
                    list: [
                      {
                        title: '巡鹰 RWA Vault（dFocre）<span class="rate">年化收益率：8%</span>',
                        desc: '三个月定期理财。',
                        showDesc:false,
                        badge: '12 月 23 日起，参与第二期的用户可提取本金及累计利息。第三期时间待定。',
                        explain: '本次 RWA 项目的底层资产为「巡鹰出行」在中国境内部署的两轮车换电基础设施。Conflux 通过香港合规实体以有限合伙人（LP）身份参与巡鹰新能源换电基础设施基金的投资。链上用户透过向金库出借稳定币获取收益，Conflux 向用户提供固定年化约 <span class="rate">8%</span> 的收益率。',
                        btnList: [
                          {
                            text: '立即参与',
                            class: 'primary',
                            alert: false,
                            link: 'https://rwa.dforce.network/#/Earn'
                          },
                          {
                            text: '操作教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/dforce-conflux-rwa-eag-s2/22736?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]

                      }
                    ]
                  },
                  {
                    title: '借贷',
                    id: 'yield-3',
                    list: [
                      {
                        title: 'AxCNH 借贷（Unitus Finance）<span class="rate">年化收益率：3.5%</span>',
                        showDesc:true,
                        desc: '通过存入 AxCNH 获取收益，随时可提取。在激励活动期间，用户借出 AxCNH 也可获得<span class="rate">4.5%</span> 的激励（需要另付平台 <span class="rate">2%</span> 利息）。',
                        badge: 'AxCNH 存款激励持续进行中，Conflux将从2 月 27 日起，在接下来的两周时间，为 Unitus Finance 上的 $AxCNH 供应方发放  120,000 $CFX奖励，并为借款方发放  55,000 $CFX 奖励。',
                        explain: 'AxCNH 是 AnchorX 发行的离岸人民币挂钩的稳定币，已经过哈萨克斯坦监管机构认证。AxCNH 上线 Unitus 后，用户可以开始存入 AxCNH。',
                        btnList: [
                          {
                            text: '立即参与',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.unitus.finance/#/lending'
                          },
                          {
                            text: '操作教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/axcnh-unitus/23000?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]
                      },
                      {
                        title: 'USDT0 借贷（Unitus Finance）',
                        showDesc:false,
                        desc: '通过存入 USDT0 获取收益，<span class="rate">随时可提取</span>',
                        badge: '',
                        explain: 'USDT0 是 Tether USDT 的跨链部署，统一了以太坊和 Conflux 等网络之间全球最大稳定币的流动性。dForce 现已全面集成 USDT0，其借贷协议 Unitus Finance 已提供相关借贷服务。',
                        btnList: [
                          {
                            text: '立即参与',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.unitus.finance/#/lending/lend/supply-USDT0'
                          },
                          // {
                          //   text: '操作教程',
                          //   class: '',
                          //   alert: false,
                          //   link: ''
                          // }
                        ]

                      }
                    ]
                  },
                  {
                    title: '流动性激励',
                    id: 'yield-4',
                    list: [
                      {
                        title: 'AxCNH–USDT0 流动性挖矿（Swappi）',
                        showDesc:false,
                        desc: '通过 LP 获取收益，<span class="rate">可随时提取</span>。',
                        badge: '',
                        explain: 'Swappi 现已正式上线 AxCNH–USDT0 流动性池。用户可通过为 AxCNH–USDT0 流动性池提供流动性获取收益。',
                        btnList: [
                          {
                            text: '立即参与',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.swappi.io/#/pool/v2'
                          },
                          {
                            text: '操作教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/swappi-usdt0-axcnh/23184?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ5MzIxNTIsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDkzMTg1MiwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05MzkxNzQ1Njg3fQ.9VBNNyyPnnf--P_xFOydkMmSRFXaMdlsyo8YbNe98YI'
                          }
                        ]
                      },
                      {
                        title: 'WallFreeX',
                        desc: '支持 Conflux 链上 USDT、CFX、AxCNH 之间的兑换。',
                        showDesc:false,
                        badge: 'WallFreeX 推出积分计划，用户通过参与链上交易或提供流动性即可获取积分，未来可凭积分拥有空投等权益。',
                        explain: 'Uniswap V3 机制的 DEX，滑点更低，USDT <> AxCNH 兑换更推荐使用。',
                        btnList: [
                          {
                            text: '立即参与',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/pool/add_liquidity'
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
            ],
            'menuList': [
              {
                title: '链上资产',
                expand: true,
                list: [
                  // {
                  //   title: '币安',
                  //   link: '#deposit-1'
                  // },
                  {
                    title: 'USDT',
                    link: '#deposit-1'
                  },
                  {
                    title: 'AxCNH',
                    link: '#deposit-2'
                  },
                  {
                    title: 'CFX',
                    link: '#deposit-3'
                  }
                ]
              },
              {
                title: '资产兑换',
                expand: true,
                list: [
                  {
                    title: 'to C',
                    link: '#withdraw-1'
                  },
                  // {
                  //   title: 'to B',
                  //   link: '#withdraw-2'
                  // }
                ]
              },
              {
                title: '理财',
                expand: true,
                list: [
                  {
                    title: 'PoS 矿池质押',
                    link: '#yield-1'
                  },
                  {
                    title: 'RWA Vault',
                    link: '#yield-2'
                  }, {
                    title: '借贷',
                    link: '#yield-3'
                  },
                  {
                    title: '流动性挖矿',
                    link: '#yield-4'
                  }
                ]
              },
              {
                title: '生态工具导航',
                expand: false,
                list: [
                  {
                    title: '区块链浏览器',
                    link: '#tools-1'
                  },
                  {
                    title: '跨链桥',
                    link: '#tools-2'
                  },
                  {
                    title: '稳定币',
                    link: '#tools-3'
                  }
                ]
              },
            ],
            'toolList': [
              {
                title: '区块链浏览器',
                id: 'tools-1',
                list: [
                  {
                    text: 'Conflux Scan',
                    class: 'primary',
                    alert: true,
                    link: 'https://www.confluxscan.org/'
                  }
                ]
              },
              {
                title: '跨链桥',
                id: 'tools-2',
                list: [
                  {
                    text: 'Meson',
                    class: 'primary',
                    alert: true,
                    link: 'https://meson.fi/'
                  },
                  {
                    text: 'KinetFlow',
                    class: 'primary',
                    alert: true,
                    link: 'https://www.kinetflow.io/'
                  },
                  {
                    text: 'Stargate',
                    class: 'primary',
                    alert: true,
                    link: 'https://stargate.finance/?srcChain=conflux&srcToken=0xaf37E8B6C9ED7f6318979f56Fc287d76c30847ff'
                  }
                ]
              },
              {
                title: '稳定币',
                id: 'tools-3',
                list: [
                  {
                    text: 'USDT0',
                    class: 'primary',
                    alert: true,
                    link: 'https://v2.confluxhub.io/usdt0'
                  },
                  {
                    text: 'AxCNH',
                    class: 'primary',
                    alert: true,
                    link: 'https://v2.confluxhub.io/ax-cnh'
                  }
                ]
              }
            ]
          },
          "en": {
            'name': 'Start Conflux',
            'eco': 'Asset Solutions',
            'tool': 'Ecosystem Resources',
            'heroTitle': 'Conflux Ecosystem Hub',
            'heroSubtitle': 'A one-stop hub for Conflux ecosystem tools and services, helping you navigate and get started with ease',
            'heroBtnText': 'Get Start',
            'firstTitle': 'Asset Solutions',
            'firstSubtitle': 'A curated overview of common asset pathways and tools in the Conflux ecosystem, enabling smooth and secure asset swaps, bridging, and on-chain management.',
            'secondTitle': 'Ecosystem Resources',
            'secondSubtitle': 'Explore core apps and resources in the Conflux ecosystem, easily access on-chain services and discover more ecosystem tools.',
            'contentList': [
              {
                title: 'On-chain Assets',
                id: 'deposit',
                content: [
                  // {
                  //   title: 'Fiat → USDT on CEXs',
                  //   id: 'deposit-1',
                  //   list: [
                  //     {
                  //       title: 'Binance',
                  //       desc: '',
                  //       showDesc:false,
                  //       badge: '',
                  //       explain:'Binance is one of the world’s leading centralized exchanges by trading volume, known for deep liquidity and a broad range of assets. It remains a primary choice for many users to purchase USDT.',
                  //       btnList: [
                  //         {
                  //           text: 'Binance Beginner’s Guide',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.binance.com/zh-CN/support/faq/detail/c780097f75dd450a82d17f1e84153276'
                  //         },
                  //         {
                  //           text: 'Binance P2P Guide',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.binance.com/zh-CN/support/faq/detail/384c0a3441b04a9cbe97c9687ef86b60'
                  //         }
                  //       ]
                  //     },
                  //     {
                  //       title: 'OKX',
                  //       desc: '',
                  //       showDesc:false,
                  //       badge: '',
                  //       explain:'OKX is one of the most widely used global centralized exchanges, offering a reliable trading experience and serving as a popular option for purchasing USDT.',
                  //       btnList: [
                  //         {
                  //           text: 'OKX Beginner’s Guide',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.okx.com/zh-hans/help/okx-beginners-guide'
                  //         }
                  //       ]
                  //     }
                  //   ]
                  // },
                  {
                    title: 'USDT on CEXs → USDT on Conflux',
                    id: 'deposit-2',
                    list: [
                      {
                        title: 'Meson',
                        desc: '<span class="rate">Zero-fee bridge</span> from CEX to Conflux eSpace for USDT0 / USDC / AxCNH.',
                        showDesc:true,
                        badge: '',
                        explain: 'Meson is the most widely used bridge in the Conflux ecosystem, supporting 50+ high-performance chains and enabling direct deposits and withdrawals from CEXs to Conflux eSpace. Supported CEXs include Binance, OKX, Bybit and Gate.io.',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://meson.fi/'
                          }, {
                            text: 'Guide: Swap',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/meson/21482?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }, {
                            text: 'CEX<>eSpace',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/meson-conflux-espace-usdt/22087?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]
                      }
                    ]
                  },
                  {
                    title: 'USDT on Conflux → AxCNH',
                    id: 'deposit-3',
                    list: [
                      {
                        title: 'WallFreeX',
                        desc: 'Supports swapping between USDT, CFX, and AxCNH on Conflux.',
                        showDesc:false,
                        badge: '',
                        explain: 'A DEX built on Uniswap V3, offering low-slippage swaps. Recommended for USDT ↔ AxCNH swaps on Conflux.',
                        btnList: [
                          {
                            text: 'Start Swapping',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/swap'
                          }
                        ]
                      }
                    ]
                  },
                  {
                    title: 'USDT on Conflux → CFX',
                    id: 'deposit-4',
                    list: [
                      {
                        title: 'Swappi',
                        desc: 'Supports swapping a variety of assets on Conflux.',
                        showDesc:false,
                        badge: '',
                        explain: 'The first DEX on Conflux eSpace, offering fast, low-cost swaps, with LP staking to earn yield.',
                        btnList: [
                          {
                            text: 'Start Swapping',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.swappi.io/#/swap'
                          }
                        ]
                      },
                      {
                        title: 'WallFreeX',
                        desc: 'Supports swapping between USDT, CFX, and AxCNH on Conflux.',
                        showDesc:false,
                        badge: '',
                        explain: 'A DEX built on Uniswap V3, offering low-slippage swaps. Recommended for USDT ↔ AxCNH swaps on Conflux.',
                        btnList: [
                          {
                            text: 'Start Swapping',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/swap'
                          }
                        ]
                      }
                    ]
                  }
                ],
              },
              {
                title: 'Asset Off-Ramps',
                id: 'withdraw',
                content: [
                  {
                    title: 'to C',
                    id: 'withdraw-1',
                    list: [
                      {
                        title: 'BitUnion',
                        desc: 'Swap USDT to fiat in one click.',
                        showDesc:false,
                        badge: '',
                        explain: 'BitUnion is a PayFi solution built on Conflux, designed to make digital assets easier to use and circulate in real-world scenarios.',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://bitunion.io/'
                          },
                          {
                            text: 'Guide',
                            class: '',
                            alert: false,
                            link: 'https://bitunion.gitbook.io/bitunion-docs'
                          }
                        ]
                      }
                    ]
                  },
                  // {
                  //   title: 'to B',
                  //   id: 'withdraw-2',
                  //   list: [
                  //     {
                  //       title: 'High-Value Trade',
                  //       desc: 'Convert AxCNH to offshore RMB and settle directly into a Hong Kong bank account.',
                  //       showDesc:false,
                  //       badge: '',
                  //       line:1,
                  //       explain: 'This service provides trade background documentation and pro forma invoices to support compliant settlement, with an exchange rate of 1:1 and a fee of 0.3%–0.5%.',
                  //       btnList: [
                  //         {
                  //           text: 'Apply',
                  //           class: 'primary',
                  //           alert: false,
                  //           link: 'https://docs.google.com/forms/d/e/1FAIpQLSd0iLl4zKRfw8RRuGXNDLkvjx_eRnCHVnV0HBThGyeteP7SuQ/viewform?usp=dialog'
                  //         }
                  //       ]
                  //     }
                  //   ]
                  // }
                ]
              },
              {
                title: 'Asset Management',
                id: 'yield',
                content: [
                  {
                    title: 'PoS Staking',
                    id: 'yield-1',
                    list: [
                      {
                        title: 'CFX Staking (ABC Pool)<span class="rate">APY：8-9%</span>',
                        desc: 'Lock-up: 13+1, rewards claimable <span class="rate">anytime</span>.',
                        showDesc:false,
                        badge: '',
                        explain: 'ABC Pool is Conflux’s largest PoS staking pool, offering coin-based yields. Price volatility is the primary risk. Single staking amounts must be in multiples of 1,000 CFX. ABC Pool charges <span class="rate">8%</span> of rewards as a service fee.',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://confluxpos.cn/'
                          },
                          {
                            text: 'Guide',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/abcpool/18125?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]

                      }
                    ]
                  },
                  {
                    title: 'RWA Vault',
                    id: 'yield-2',
                    list: [
                      {
                        title: 'EAG RWA Vault (dForce)<span class="rate">APY：8%</span>',
                        desc: '3-month fixed-term yield.',
                        showDesc:false,
                        badge: 'Starting December 23, users participating in Phase 2 may withdraw both their principal and accumulated interest. Phase 3 timing is TBD.',
                        explain: 'The underlying assets of this RWA project are Eagle’s two-wheeler battery swap infrastructure deployed across China. Conflux participates as an LP in the Eagle New Energy Battery Swap Infrastructure Fund via a compliant Hong Kong-based entity. On-chain users can deposit stablecoins into the vault and earn a fixed APY of approximately <span class="rate">8%</span>.',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://rwa.dforce.network/#/Earn'
                          },
                          {
                            text: 'Guide',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/dforce-conflux-rwa-eag-s2/22736?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]

                      }
                    ]
                  },
                  {
                    title: 'Lending & Borrowing',
                    id: 'yield-3',
                    list: [
                      {
                        title: 'AxCNH Lending (Unitus Finance)<span class="rate">APY：3.5%</span>',
                        desc: 'Earn yield by depositing AxCNH, withdraw anytime. During the incentive campaign, users can also earn <span class="rate">4.5%</span> rewards for lending AxCNH (an additional <span class="rate">2%</span> interest payable to the platform).',
                        showDesc:true,
                        badge: 'AxCNH lending incentives are ongoing. From Feb 27, Conflux will distribute 120,000 $CFX rewards to suppliers of $AxCNH on Unitus Finance and 55,000 $CFX to borrowers over two weeks.',
                        explain: 'AxCNH is an offshore RMB-pegged stablecoin issued by AnchorX and regulated in Kazakhstan. Users can now deposit AxCNH on Unitus',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.unitus.finance/#/lending'
                          },
                          {
                            text: 'Guide',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/axcnh-unitus/23000?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]
                      },
                      {
                        title: 'USDT0 Lending (Unitus Finance)',
                        desc: 'Earn yield by depositing USDT0, withdraw <span class="rate">anytime</span>.',
                        showDesc:false,
                        badge: '',
                        explain: 'USDT0 is built on LayerZero’s Omnichain Fungible Token (OFT) standard, designed to create a unified layer of cross-chain liquidity. USDT0 is now fully integrated into dForce ecosystem. And Unitus Finance already launched USDT0 lending pool on Conflux eSpace.',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.unitus.finance/#/lending/lend/supply-USDT0'
                          },
                          // {
                          //   text: '操作教程',
                          //   class: '',
                          //   alert: false,
                          //   link: ''
                          // }
                        ]

                      }
                    ]
                  },
                  {
                    title: 'LP Staking',
                    id: 'yield-4',
                    list: [
                      {
                        title: 'AxCNH–USDT0 LP Staking (Swappi)',
                        desc: 'Earn yield by providing liquidity, withdraw <span class="rate">anytime</span>.',
                        showDesc:false,
                        badge: '',
                        explain: 'Swappi has officially launched the AxCNH–USDT0 liquidity pool. Users can earn yield by providing liquidity to this pool.',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.swappi.io/#/pool/v2'
                          },
                          {
                            text: 'Guide',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/swappi-usdt0-axcnh/23184?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ5MzIxNTIsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDkzMTg1MiwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05MzkxNzQ1Njg3fQ.9VBNNyyPnnf--P_xFOydkMmSRFXaMdlsyo8YbNe98YI'
                          }
                        ]
                      },
                      {
                        title: 'WallFreeX',
                        desc: 'Supports swapping between USDT, CFX, and AxCNH on Conflux.',
                        badge: 'WallFreeX has launched a points program. Users can earn points by swapping on-chain or providing liquidity, which can later be redeemed for airdrops and other rewards.',
                        explain: 'A DEX built on Uniswap V3, offering low-slippage swaps. Recommended for USDT ↔ AxCNH swaps on Conflux.',
                        btnList: [
                          {
                            text: 'Get Started',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/pool/add_liquidity'
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
            ],
            'menuList': [
              {
                title: 'On-chain Assets',
                expand: true,
                list: [
                  // {
                  //   title: 'Binance',
                  //   link: '#deposit-1'
                  // },
                  {
                    title: 'USDT',
                    link: '#deposit-1'
                  },
                  {
                    title: 'AxCNH',
                    link: '#deposit-2'
                  },
                  {
                    title: 'CFX',
                    link: '#deposit-3'
                  }
                ]
              },
              {
                title: 'Asset Off-Ramps',
                expand: true,
                list: [
                  {
                    title: 'to C',
                    link: '#withdraw-1'
                  },
                  // {
                  //   title: 'to B',
                  //   link: '#withdraw-2'
                  // }
                ]
              },
              {
                title: 'Asset Management',
                expand: true,
                list: [
                  {
                    title: 'PoS Staking',
                    link: '#yield-1'
                  },
                  {
                    title: 'RWA Vault',
                    link: '#yield-2'
                  }, {
                    title: 'Lending & Borrowing',
                    link: '#yield-3'
                  },
                  {
                    title: 'LP Staking',
                    link: '#yield-4'
                  }
                ]
              },
              {
                title: 'Ecosystem Resources',
                expand: false,
                list: [
                  {
                    title: 'Blockchain Explorer',
                    link: '#tools-1'
                  },
                  {
                    title: 'Bridges',
                    link: '#tools-2'
                  },
                  {
                    title: 'Stablecoins',
                    link: '#tools-3'
                  }
                ]
              },
            ],
            'toolList': [
              {
                title: 'Blockchain Explorer',
                id: 'tools-1',
                list: [
                  {
                    text: 'Conflux Scan',
                    class: 'primary',
                    alert: true,
                    link: 'https://www.confluxscan.org/'
                  }
                ]
              },
              {
                title: 'Bridges',
                id: 'tools-2',
                list: [
                  {
                    text: 'Meson',
                    class: 'primary',
                    alert: true,
                    link: 'https://meson.fi/'
                  },
                  {
                    text: 'KinetFlow',
                    class: 'primary',
                    alert: true,
                    link: 'https://www.kinetflow.io/'
                  },
                  {
                    text: 'Stargate',
                    class: 'primary',
                    alert: true,
                    link: 'https://stargate.finance/?srcChain=conflux&srcToken=0xaf37E8B6C9ED7f6318979f56Fc287d76c30847ff'
                  }
                ]
              },
              {
                title: 'Stablecoins',
                id: 'tools-3',
                list: [
                  {
                    text: 'USDT0',
                    class: 'primary',
                    alert: true,
                    link: 'https://v2.confluxhub.io/usdt0'
                  },
                  {
                    text: 'AxCNH',
                    class: 'primary',
                    alert: true,
                    link: 'https://v2.confluxhub.io/ax-cnh'
                  }
                ]
              }
            ]
          },
          "zh-TW": {
            'name': 'Start Conflux',
            'eco': '服務指引',
            'tool': '工具導航',
            'heroTitle': 'Conflux 生態服務指南',
            'heroSubtitle': '一站式匯集鏈上生態與實用工具，幫助你快速了解並使用 Conflux 生態服務',
            'heroBtnText': '立即開始',
            'firstTitle': '資金服務指引',
            'firstSubtitle': '集中展示 Conflux 生態中與資產流轉相關的常見路徑與工具，幫助用戶順暢、安全地完成資金轉換與鏈上資產管理。',
            'secondTitle': '生態工具導航',
            'secondSubtitle': '整理 Conflux 生態內常用的應用與工具入口，方便用戶訪問鏈上服務並探索更多生態資源。',
            'contentList': [
              {
                title: '鏈上資產',
                id: 'deposit',
                content: [
                  // {
                  //   title: '法幣 → 交易所中的 USDT',
                  //   id: 'deposit-1',
                  //   list: [
                  //     {
                  //       title: '幣安',
                  //       desc: '',
                  //       showDesc:false,
                  //       badge: '',
                  //       explain:'幣安是全球交易量長期領先的中心化交易所之一，流動性充足、幣種豐富，是許多用戶購買 USDT 的主要選擇。',
                  //       btnList: [
                  //         {
                  //           text: '幣安初學者指南',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.binance.com/zh-CN/support/faq/detail/c780097f75dd450a82d17f1e84153276'
                  //         },
                  //         {
                  //           text: '幣安 C2C 教程',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.binance.com/zh-CN/support/faq/detail/384c0a3441b04a9cbe97c9687ef86b60'
                  //         }
                  //       ]
                  //     },
                  //     {
                  //       title: 'OKX',
                  //       desc: '',
                  //       showDesc:false,
                  //       badge: '',
                  //       explain:'OKX 是全球用戶覆蓋最廣的中心化交易所之一，提供穩定的交易體驗，也是用戶購買 USDT 時最常使用的渠道之一。',
                  //       btnList: [
                  //         {
                  //           text: 'OKX 新手攻略',
                  //           class: '',
                  //           alert: true,
                  //           link: 'https://www.okx.com/zh-hans/help/okx-beginners-guide'
                  //         }
                  //       ]
                  //     }
                  //   ]
                  // },
                  {
                    title: '交易所中的 USDT → Conflux 鏈上的 USDT',
                    id: 'deposit-2',
                    list: [
                      {
                        title: 'Meson',
                        desc: '支持交易所一鍵充提 Conflux eSpace 的 USDT0 / USDC / AxCNH，<span class="rate">0 手續費。</span>',
                        showDesc:true,
                        badge: '',
                        explain: 'Meson 是目前 Conflux 鏈上使用最多的跨鏈橋，已支持 50+ 條主流高性能區塊鏈。用戶可直接在交易所充提 Conflux eSpace 上的 USDT。支持的交易所包括 Binance、OKX、Bybit、Gate.io 等。',
                        btnList: [
                          {
                            text: '去跨鏈',
                            class: 'primary',
                            alert: false,
                            link: 'https://meson.fi/'
                          }, {
                            text: '跨鏈教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/meson/21482?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }, {
                            text: '交易所充提教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/meson-conflux-espace-usdt/22087?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]
                      }
                    ]
                  },
                  {
                    title: 'Conflux 鏈上的 USDT → AxCNH',
                    id: 'deposit-3',
                    list: [
                      {
                        title: 'WallFreeX',
                        desc: '支持 Conflux 鏈上 USDT、CFX、AxCNH 之間的兌換。',
                        showDesc:false,
                        badge: '',
                        explain: '採用 Uniswap V3 機制的 DEX，滑點更低，USDT ↔ AxCNH 兌換更推薦使用。',
                        btnList: [
                          {
                            text: '去兌換',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/swap'
                          }
                        ]
                      }
                    ]
                  },
                  {
                    title: 'Conflux 鏈上的 USDT → CFX',
                    id: 'deposit-4',
                    list: [
                      {
                        title: 'Swappi',
                        desc: '支持 Conflux 鏈上各種資產的兌換。',
                        showDesc:false,
                        badge: '',
                        explain: 'Conflux eSpace 的首個 DEX，提供快速、低成本的代幣兌換及流動性挖礦服務。',
                        btnList: [
                          {
                            text: '去兌換',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.swappi.io/#/swap'
                          }
                        ]
                      },
                      {
                        title: 'WallFreeX',
                        desc: '支持 Conflux 鏈上 USDT、CFX、AxCNH 之間的兌換。',
                        showDesc:false,
                        badge: '',
                        explain: '採用 Uniswap V3 機制的 DEX，滑點更低，USDT ↔ AxCNH 兌換更推薦使用。',
                        btnList: [
                          {
                            text: '去兌換',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/swap'
                          }
                        ]
                      }
                    ]
                  }
                ],
              },
              {
                title: '資產兌換',
                id: 'withdraw',
                content: [
                  {
                    title: 'to C',
                    id: 'withdraw-1',
                    list: [
                      {
                        title: 'BitUnion',
                        desc: '一鍵將 USDT 轉換為法幣。',
                        showDesc:false,
                        badge: '',
                        explain: 'BitUnion 是 Conflux 鏈上的 PayFi 方案，致力於協助數字資產實現便捷流通與廣泛應用。',
                        btnList: [
                          {
                            text: '全球速匯',
                            class: 'primary',
                            alert: false,
                            link: 'https://bitunion.io/'
                          },
                          {
                            text: '使用教程',
                            class: '',
                            alert: false,
                            link: 'https://bitunion.gitbook.io/bitunion-docs'
                          }
                        ]
                      }
                    ]
                  },
                  // {
                  //   title: 'to B',
                  //   id: 'withdraw-2',
                  //   list: [
                  //     {
                  //       title: '大額貿易',
                  //       desc: '將 AxCNH 轉換為香港銀行帳戶的離岸人民幣。',
                  //       showDesc:false,
                  //       badge: '',
                  //       line:1,
                  //       explain: '匯率 1:1，手續費：<span class="rate">0.3%-0.5%</span>，提供貿易背景文件與形式發票',
                  //       btnList: [
                  //         {
                  //           text: '去合作',
                  //           class: 'primary',
                  //           alert: false,
                  //           link: 'https://docs.google.com/forms/d/e/1FAIpQLSd0iLl4zKRfw8RRuGXNDLkvjx_eRnCHVnV0HBThGyeteP7SuQ/viewform?usp=dialog'
                  //         }
                  //       ]
                  //     }
                  //   ]
                  // }
                ]
              },
              {
                title: '理財',
                id: 'yield',
                content: [
                  {
                    title: 'PoS 礦池質押',
                    id: 'yield-1',
                    list: [
                      {
                        title: 'CFX 质押（ABC 矿池）<span class="rate">年化收益率：8-9%</span>',
                        desc: '鎖倉期 13+1，獎勵<span class="rate">隨時可領取。</span>',
                        showDesc:false,
                        badge: '',
                        explain: 'ABC 矿池是 Conflux 鏈上最大的 PoS 礦池。PoS 質押為幣本位理財，主要風險來自幣價波動。單次質押量需為 1000 CFX 的整數倍，ABC 矿池收取獎勵的<span class="rate">8%</span> 作為手續費。',
                        btnList: [
                          {
                            text: '去質押',
                            class: 'primary',
                            alert: false,
                            link: 'https://confluxpos.cn/'
                          },
                          {
                            text: '質押教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/abcpool/18125?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]

                      }
                    ]
                  },
                  {
                    title: 'RWA Vault',
                    id: 'yield-2',
                    list: [
                      {
                        title: '巡鷹 RWA Vault（dForce）<span class="rate">年化收益率：8%</span>',
                        desc: '三個月定期理財。',
                        showDesc:false,
                        badge: '自 12 月 23 日起，參與第二期的用戶可提取本金及累計利息。第三期時間待定。',
                        explain: '本次 RWA 項目的底層資產為「巡鷹出行」在中國境內部署的兩輪車換電基礎設施。Conflux 透過香港合規實體以有限合夥人（LP）身份參與巡鷹新能源換電基礎設施基金的投資。鏈上用戶可向金庫出借穩定幣以獲取收益，Conflux 向用戶提供固定年化約<span class="rate">8%</span> 的收益率。',
                        btnList: [
                          {
                            text: '立即參與',
                            class: 'primary',
                            alert: false,
                            link: 'https://rwa.dforce.network/#/Earn'
                          },
                          {
                            text: '操作教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/dforce-conflux-rwa-eag-s2/22736?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]

                      }
                    ]
                  },
                  {
                    title: '借貸',
                    id: 'yield-3',
                    list: [
                      {
                        title: 'AxCNH 借貸（Unitus Finance）<span class="rate">年化收益率：3.5%</span>',
                        desc: '透過存入 AxCNH 獲取收益，可隨時提取。在激勵活動期間，用戶借出 AxCNH 也可獲得 <span class="rate">4.5%</span> 的激勵（需另付平台 <span class="rate">2%</span> 利息）。',
                        showDesc:true,
                        badge: 'AxCNH 存款激勵持續進行中。Conflux 將自 2 月 27 日起，在接下來兩週內向 Unitus Finance 上的 $AxCNH 供應方發放 120,000 $CFX 獎勵，並向借款方發放 55,000 $CFX 獎勵。',
                        explain: 'AxCNH 是 AnchorX 發行的離岸人民幣掛鉤的穩定幣，已獲哈薩克斯坦監管機構認證。AxCNH 上線 Unitus 後，用戶可開始存入 AxCNH。',
                        btnList: [
                          {
                            text: '立即參與',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.unitus.finance/#/lending'
                          },
                          {
                            text: '操作教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/axcnh-unitus/23000?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ2NjUxNjQsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDY2NDg2NCwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05Mzg3MzQ3ODE5fQ.xHJZPTeDWpoqeh-bBHmPjoCUp-MsKSzIq0e_R7e7Iec'
                          }
                        ]
                      },
                      {
                        title: 'USDT0 借貸（Unitus Finance）',
                        desc: '通過存入 USDT0 獲取收益，<span class="rate">隨時可提取</span>。',
                        badge: '',
                        explain: 'USDT0 是 Tether USDT 的跨鏈部署，統一了以太坊和 Conflux 等網絡間全球最大穩定幣的流動性。dForce 已全面集成 USDT0，其借貸協議 Unitus Finance 已提供相關服務。',
                        btnList: [
                          {
                            text: '立即參與',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.unitus.finance/#/lending/lend/supply-USDT0'
                          },
                          // {
                          //   text: '操作教程',
                          //   class: '',
                          //   alert: false,
                          //   link: ''
                          // }
                        ]

                      }
                    ]
                  },
                  {
                    title: '流動性激勵',
                    id: 'yield-4',
                    list: [
                      {
                        title: 'AxCNH–USDT0 流動性挖礦（Swappi）',
                        desc: '通過提供流動性獲取收益，<span class="rate">隨時可提取</span>。',
                        showDesc:false,
                        badge: '',
                        explain: 'Swappi 已正式上線 AxCNH–USDT0 流動性池。用戶可通過為 AxCNH–USDT0 流動性池提供流動性獲得收益。',
                        btnList: [
                          {
                            text: '立即參與',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.swappi.io/#/pool/v2'
                          },
                          {
                            text: '操作教程',
                            class: '',
                            alert: false,
                            link: 'https://forum.conflux.fun/t/swappi-usdt0-axcnh/23184?accessToken=eyJhbGciOiJIUzI1NiIsImtpZCI6ImRlZmF1bHQiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE3NjQ5MzIxNTIsImZpbGVHVUlEIjoibTVrdmRkMExwUGZOeW0zWCIsImlhdCI6MTc2NDkzMTg1MiwiaXNzIjoidXBsb2FkZXJfYWNjZXNzX3Jlc291cmNlIiwicGFhIjoiYWxsOmFsbDoiLCJ1c2VySWQiOi05MzkxNzQ1Njg3fQ.9VBNNyyPnnf--P_xFOydkMmSRFXaMdlsyo8YbNe98YI'
                          }
                        ]
                      },
                      {
                        title: 'WallFreeX',
                        desc: '支持 Conflux 鏈上 USDT、CFX、AxCNH 之間的兌換。',
                        showDesc:false,
                        badge: 'WallFreeX 推出積分計畫，用戶透過鏈上交易或提供流動性即可獲得積分，日後可憑積分擁有空投及其他權益。',
                        explain: '採用 Uniswap V3 機制的 DEX，滑點更低，USDT ↔ AxCNH 兌換更推薦使用。',
                        btnList: [
                          {
                            text: '立即參與',
                            class: 'primary',
                            alert: false,
                            link: 'https://app.wallfreex.com/pool/add_liquidity'
                          }
                        ]
                      }
                    ]
                  }
                ]
              },
            ],
            'menuList': [
              {
                title: '鏈上資產',
                expand: true,
                list: [
                  // {
                  //   title: 'Binance',
                  //   link: '#deposit-1'
                  // },
                  {
                    title: 'USDT',
                    link: '#deposit-1'
                  },
                  {
                    title: 'AxCNH',
                    link: '#deposit-2'
                  },
                  {
                    title: 'CFX',
                    link: '#deposit-3'
                  }
                ]
              },
              {
                title: '資產兌換',
                expand: true,
                list: [
                  {
                    title: 'to C',
                    link: '#withdraw-1'
                  },
                  // {
                  //   title: 'to B',
                  //   link: '#withdraw-2'
                  // }
                ]
              },
              {
                title: '理財',
                expand: true,
                list: [
                  {
                    title: 'PoS 礦池質押',
                    link: '#yield-1'
                  },
                  {
                    title: 'RWA Vault',
                    link: '#yield-2'
                  }, {
                    title: '借貸',
                    link: '#yield-3'
                  },
                  {
                    title: '流動性激勵',
                    link: '#yield-4'
                  }
                ]
              },
              {
                title: '生態工具導航',
                expand: false,
                list: [
                  {
                    title: '區塊鏈瀏覽器',
                    link: '#tools-1'
                  },
                  {
                    title: '跨鏈橋',
                    link: '#tools-2'
                  },
                  {
                    title: '穩定幣',
                    link: '#tools-3'
                  }
                ]
              },
            ],
            'toolList': [
              {
                title: '區塊鏈瀏覽器',
                id: 'tools-1',
                list: [
                  {
                    text: 'Conflux Scan',
                    class: 'primary',
                    alert: true,
                    link: 'https://www.confluxscan.org/'
                  }
                ]
              },
              {
                title: '跨鏈橋',
                id: 'tools-2',
                list: [
                  {
                    text: 'Meson',
                    class: 'primary',
                    alert: true,
                    link: 'https://meson.fi/'
                  },
                  {
                    text: 'KinetFlow',
                    class: 'primary',
                    alert: true,
                    link: 'https://www.kinetflow.io/'
                  },
                  {
                    text: 'Stargate',
                    class: 'primary',
                    alert: true,
                    link: 'https://stargate.finance/?srcChain=conflux&srcToken=0xaf37E8B6C9ED7f6318979f56Fc287d76c30847ff'
                  }
                ]
              },
              {
                title: '穩定幣',
                id: 'tools-3',
                list: [
                  {
                    text: 'USDT0',
                    class: 'primary',
                    alert: true,
                    link: 'https://v2.confluxhub.io/usdt0'
                  },
                  {
                    text: 'AxCNH',
                    class: 'primary',
                    alert: true,
                    link: 'https://v2.confluxhub.io/ax-cnh'
                  }
                ]
              }
            ]
          }
        };